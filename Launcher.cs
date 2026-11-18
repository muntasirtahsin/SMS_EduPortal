using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Threading;
using System.Windows.Forms;

internal static class Program
{
    private const string PayloadResource = "EduPortalPayload.zip";

    [STAThread]
    private static void Main()
    {
        try
        {
            string localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            if (String.IsNullOrEmpty(localAppData))
            {
                throw new InvalidOperationException("Windows Local AppData is unavailable.");
            }

            string dataDirectory = Path.Combine(localAppData, "EduPortal");
            string appDirectory = Path.Combine(dataDirectory, "app");
            using (Mutex mutex = new Mutex(false, "Local\\EduPortalBootstrap"))
            {
                bool acquired;
                try
                {
                    acquired = mutex.WaitOne(TimeSpan.FromMinutes(2));
                }
                catch (AbandonedMutexException)
                {
                    acquired = true;
                }

                if (!acquired)
                {
                    throw new TimeoutException("Another EduPortal setup is still running.");
                }

                try
                {
                    if (!IsPayloadReady(appDirectory))
                    {
                        InstallPayload(dataDirectory, appDirectory);
                    }
                }
                finally
                {
                    mutex.ReleaseMutex();
                }
            }

            if (!IsPayloadReady(appDirectory))
            {
                throw new InvalidOperationException("The bundled EduPortal files could not be installed.");
            }

            ProcessStartInfo startInfo = new ProcessStartInfo();
            startInfo.FileName = Path.Combine(appDirectory, "runtime", "bin", "javaw.exe");
            startInfo.WorkingDirectory = appDirectory;
            startInfo.Arguments = "--module-path=lib " +
                "--add-modules=javafx.controls,javafx.fxml,javafx.graphics " +
                "--enable-native-access=ALL-UNNAMED " +
                "-cp \"EduPortal.jar;lib/*\" com.eduportal.Main";
            startInfo.UseShellExecute = false;
            startInfo.CreateNoWindow = true;
            using (Process process = Process.Start(startInfo))
            {
                if (process == null)
                {
                    throw new InvalidOperationException("The bundled Java runtime could not be started.");
                }
            }
        }
        catch (Exception exception)
        {
            MessageBox.Show(
                "EduPortal could not start.\n\n" + exception.Message,
                "EduPortal",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error);
        }
    }

    private static bool IsPayloadReady(string directory)
    {
        return File.Exists(Path.Combine(directory, "EduPortal.jar"))
            && File.Exists(Path.Combine(directory, "runtime", "bin", "javaw.exe"))
            && File.Exists(Path.Combine(directory, "lib", "javafx-controls-21.0.2-win.jar"))
            && File.Exists(Path.Combine(directory, "lib", "sqlite-jdbc.jar"));
    }

    private static void InstallPayload(string dataDirectory, string appDirectory)
    {
        Directory.CreateDirectory(dataDirectory);
        string stagingDirectory = Path.Combine(dataDirectory, "staging-" + Guid.NewGuid().ToString("N"));
        string archivePath = Path.Combine(Path.GetTempPath(), "EduPortal-" + Guid.NewGuid().ToString("N") + ".zip");

        try
        {
            using (Stream payload = Assembly.GetExecutingAssembly().GetManifestResourceStream(PayloadResource))
            {
                if (payload == null)
                {
                    throw new InvalidOperationException("The embedded EduPortal package is missing.");
                }
                using (FileStream archive = File.Create(archivePath))
                {
                    payload.CopyTo(archive);
                }
            }

            Directory.CreateDirectory(stagingDirectory);
            ZipFile.ExtractToDirectory(archivePath, stagingDirectory);
            if (!IsPayloadReady(stagingDirectory))
            {
                throw new InvalidDataException("The embedded EduPortal package is incomplete.");
            }

            CopyDirectory(stagingDirectory, appDirectory);
        }
        finally
        {
            if (Directory.Exists(stagingDirectory))
            {
                Directory.Delete(stagingDirectory, true);
            }
            if (File.Exists(archivePath))
            {
                File.Delete(archivePath);
            }
        }
    }

    private static void CopyDirectory(string source, string destination)
    {
        Directory.CreateDirectory(destination);
        foreach (string file in Directory.GetFiles(source))
        {
            File.Copy(file, Path.Combine(destination, Path.GetFileName(file)), true);
        }
        foreach (string directory in Directory.GetDirectories(source))
        {
            CopyDirectory(directory, Path.Combine(destination, Path.GetFileName(directory)));
        }
    }
}
