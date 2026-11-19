using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Text;
using System.Threading;
using System.Windows.Forms;

internal static class Program
{
    private const string PayloadResource = "EduPortalPayload.zip";

    [STAThread]
    private static void Main()
    {
        string appDirectory = null;
        string dataDirectory = null;
        bool applicationExited = false;
        try
        {
            string localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            if (String.IsNullOrEmpty(localAppData))
            {
                throw new InvalidOperationException("Windows Local AppData is unavailable.");
            }

            dataDirectory = Path.Combine(localAppData, "EduPortal");
            Directory.CreateDirectory(dataDirectory);
            PreserveLegacyUserFiles(dataDirectory);
            appDirectory = Path.Combine(Path.GetTempPath(), "EduPortal-" + Guid.NewGuid().ToString("N"));
            string controlPath = Path.Combine(appDirectory, "launcher-control.txt");
            string updateMarker = controlPath + ".updating";
            string executableUpdateMarker = controlPath + ".exe-update";
            InstallPayload(appDirectory);

            if (!IsPayloadReady(appDirectory))
            {
                throw new InvalidOperationException("The bundled EduPortal files could not be extracted.");
            }

            File.WriteAllText(controlPath, "STARTING");
            ProcessStartInfo startInfo = new ProcessStartInfo();
            startInfo.FileName = Path.Combine(appDirectory, "runtime", "bin", "javaw.exe");
            startInfo.WorkingDirectory = appDirectory;
            startInfo.Arguments = "--module-path=lib " +
                "--add-modules=javafx.controls,javafx.fxml,javafx.graphics " +
                "--enable-native-access=ALL-UNNAMED " +
                "-cp \"EduPortal.jar;lib/*\" com.eduportal.Main";
            startInfo.UseShellExecute = false;
            startInfo.CreateNoWindow = true;
            startInfo.EnvironmentVariables["EDUPORTAL_DATA_DIR"] = dataDirectory;
            startInfo.EnvironmentVariables["EDUPORTAL_LAUNCHER_CONTROL"] = controlPath;
            string javaOutputPath = Path.Combine(dataDirectory, "java-output.log");
            string startupReadyFile = Environment.GetEnvironmentVariable("EDUPORTAL_UPDATE_READY_FILE");
            if (!String.IsNullOrEmpty(startupReadyFile))
            {
                startInfo.EnvironmentVariables["EDUPORTAL_UPDATE_READY_FILE"] = startupReadyFile;
            }
            startInfo.RedirectStandardOutput = true;
            startInfo.RedirectStandardError = true;
            using (StreamWriter javaOutput = new StreamWriter(javaOutputPath, true))
            using (StreamWriter javaError = new StreamWriter(Path.Combine(dataDirectory, "java-error.log"), true))
            using (Process process = new Process())
            {
                javaOutput.AutoFlush = true;
                javaError.AutoFlush = true;
                process.StartInfo = startInfo;
                process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs eventArgs)
                {
                    if (eventArgs.Data != null) javaOutput.WriteLine(eventArgs.Data);
                };
                process.ErrorDataReceived += delegate(object sender, DataReceivedEventArgs eventArgs)
                {
                    if (eventArgs.Data != null) javaError.WriteLine(eventArgs.Data);
                };
                if (!process.Start())
                {
                    throw new InvalidOperationException("The bundled Java runtime could not be started.");
                }
                process.BeginOutputReadLine();
                process.BeginErrorReadLine();
                if (!File.Exists(updateMarker))
                {
                    File.WriteAllText(controlPath, process.Id.ToString());
                }
                WaitForApplicationExit(process.Id, controlPath, updateMarker);
                process.WaitForExit();
                File.AppendAllText(
                    Path.Combine(dataDirectory, "launcher-error.log"),
                    DateTime.Now.ToString("s") + " Java process " + process.Id
                        + " exited with code " + process.ExitCode + Environment.NewLine);
                applicationExited = true;
            }
            if (File.Exists(executableUpdateMarker))
            {
                string stagedExecutable = File.ReadAllText(executableUpdateMarker).Trim();
                ScheduleExecutableReplacement(stagedExecutable, dataDirectory);
                File.Delete(executableUpdateMarker);
            }
        }
        catch (Exception exception)
        {
            if (dataDirectory != null)
            {
                try
                {
                    File.AppendAllText(
                        Path.Combine(dataDirectory, "launcher-error.log"),
                        DateTime.Now.ToString("s") + " " + exception + Environment.NewLine);
                }
                catch (IOException)
                {
                }
            }
            MessageBox.Show(
                "EduPortal could not start.\n\n" + exception.Message,
                "EduPortal",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error);
        }
        finally
        {
            if (appDirectory != null && Directory.Exists(appDirectory))
            {
                try
                {
                    Directory.Delete(appDirectory, true);
                }
                catch (IOException exception)
                {
                    ShowCleanupWarning(appDirectory, exception);
                }
                catch (UnauthorizedAccessException exception)
                {
                    ShowCleanupWarning(appDirectory, exception);
                }
            }
            if (applicationExited && dataDirectory != null)
            {
                RemoveLegacyApplicationFiles(dataDirectory);
            }
        }
    }

    private static bool IsPayloadReady(string directory)
    {
        return File.Exists(Path.Combine(directory, "EduPortal.jar"))
            && File.Exists(Path.Combine(directory, "runtime", "bin", "javaw.exe"))
            && File.Exists(Path.Combine(directory, "lib", "javafx-controls-21.0.2-win.jar"))
            && File.Exists(Path.Combine(directory, "lib", "sqlite-jdbc.jar"));
    }

    private static void InstallPayload(string appDirectory)
    {
        Directory.CreateDirectory(appDirectory);
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

            ZipFile.ExtractToDirectory(archivePath, appDirectory);
            if (!IsPayloadReady(appDirectory))
            {
                throw new InvalidDataException("The embedded EduPortal package is incomplete.");
            }
        }
        finally
        {
            if (File.Exists(archivePath))
            {
                File.Delete(archivePath);
            }
        }
    }

    private static void PreserveLegacyUserFiles(string dataDirectory)
    {
        string legacyDirectory = Path.Combine(dataDirectory, "app");
        foreach (string fileName in new[] { ".env", "config.properties" })
        {
            string legacyFile = Path.Combine(legacyDirectory, fileName);
            string persistentFile = Path.Combine(dataDirectory, fileName);
            if (File.Exists(legacyFile) && !File.Exists(persistentFile))
            {
                File.Copy(legacyFile, persistentFile);
            }
        }
    }

    private static void RemoveLegacyApplicationFiles(string dataDirectory)
    {
        string legacyDirectory = Path.Combine(dataDirectory, "app");
        if (!Directory.Exists(legacyDirectory))
        {
            return;
        }
        try
        {
            Directory.Delete(legacyDirectory, true);
        }
        catch (IOException exception)
        {
            ShowCleanupWarning(legacyDirectory, exception);
        }
        catch (UnauthorizedAccessException exception)
        {
            ShowCleanupWarning(legacyDirectory, exception);
        }
    }

    private static void WaitForApplicationExit(int initialProcessId, string controlPath, string updateMarker)
    {
        int processId = initialProcessId;
        while (true)
        {
            WaitForProcessExit(processId);
            DateTime updateDeadline = DateTime.UtcNow.AddMinutes(2);
            while (File.Exists(updateMarker))
            {
                string pendingControlValue = File.Exists(controlPath) ? File.ReadAllText(controlPath).Trim() : "";
                int restartedProcessId;
                if (Int32.TryParse(pendingControlValue, out restartedProcessId) && restartedProcessId != processId)
                {
                    processId = restartedProcessId;
                    break;
                }
                if (DateTime.UtcNow >= updateDeadline)
                {
                    throw new TimeoutException("The updater did not finish restarting EduPortal.");
                }
                Thread.Sleep(250);
            }

            string controlValue = File.Exists(controlPath) ? File.ReadAllText(controlPath).Trim() : "";
            int nextProcessId;
            if (Int32.TryParse(controlValue, out nextProcessId) && nextProcessId != processId)
            {
                processId = nextProcessId;
                continue;
            }
            if (File.Exists(updateMarker))
            {
                continue;
            }
            return;
        }
    }

    private static void WaitForProcessExit(int processId)
    {
        try
        {
            using (Process process = Process.GetProcessById(processId))
            {
                process.WaitForExit();
            }
        }
        catch (ArgumentException)
        {
        }
    }

    private static void ScheduleExecutableReplacement(string stagedExecutable, string dataDirectory)
    {
        string updateDirectory = Path.GetFullPath(Path.Combine(dataDirectory, "updates"));
        string stagedPath = Path.GetFullPath(stagedExecutable);
        if (!stagedPath.StartsWith(updateDirectory + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase)
            || !File.Exists(stagedPath))
        {
            throw new InvalidDataException("The staged application update is missing or outside the update folder.");
        }

        using (FileStream update = File.OpenRead(stagedPath))
        {
            if (update.ReadByte() != 'M' || update.ReadByte() != 'Z')
            {
                throw new InvalidDataException("The staged update is not a valid Windows application.");
            }
        }

        string targetPath = Assembly.GetExecutingAssembly().Location;
        string helperPath = Path.Combine(Path.GetTempPath(), "EduPortal-self-update-" + Guid.NewGuid().ToString("N") + ".ps1");
        string script =
            "$ErrorActionPreference = 'Stop'\n"
            + "$launcherPid = " + Process.GetCurrentProcess().Id + "\n"
            + "$target = " + PowerShellQuote(targetPath) + "\n"
            + "$staged = " + PowerShellQuote(stagedPath) + "\n"
            + "$replacement = $target + '.new'\n"
            + "$backup = $target + '.backup-' + [Guid]::NewGuid().ToString('N')\n"
            + "$failed = $target + '.failed-' + [Guid]::NewGuid().ToString('N')\n"
            + "$log = Join-Path $env:LOCALAPPDATA 'EduPortal\\update-error.log'\n"
            + "$ready = Join-Path (Join-Path $env:LOCALAPPDATA 'EduPortal\\updates') ('launcher-ready-' + [Guid]::NewGuid().ToString('N') + '.txt')\n"
            + "function Write-UpdateLog($message) {\n"
            + "  Add-Content -LiteralPath $log -Value ((Get-Date).ToString('s') + ' ' + $message)\n"
            + "}\n"
            + "try {\n"
            + "  Write-UpdateLog 'Waiting for the current launcher to exit.'\n"
            + "  while (Get-Process -Id $launcherPid -ErrorAction SilentlyContinue) { Start-Sleep -Milliseconds 250 }\n"
            + "  if (-not (Test-Path -LiteralPath $staged -PathType Leaf)) { throw 'The downloaded update file is missing.' }\n"
            + "  Copy-Item -LiteralPath $staged -Destination $replacement -Force\n"
            + "  $replaced = $false\n"
            + "  for ($attempt = 1; $attempt -le 10; $attempt++) {\n"
            + "    try {\n"
            + "      [System.IO.File]::Replace($replacement, $target, $backup)\n"
            + "      $replaced = $true\n"
            + "      break\n"
            + "    } catch {\n"
            + "      if ($attempt -eq 10) { throw }\n"
            + "      Start-Sleep -Milliseconds 500\n"
            + "    }\n"
            + "  }\n"
            + "  if (-not $replaced) { throw 'Could not replace the application executable.' }\n"
            + "  Write-UpdateLog 'Updated executable installed; restarting EduPortal.'\n"
            + "  $startInfo = New-Object System.Diagnostics.ProcessStartInfo\n"
            + "  $startInfo.FileName = $target\n"
            + "  $startInfo.WorkingDirectory = Split-Path -Parent $target\n"
            + "  $startInfo.UseShellExecute = $false\n"
            + "  $startInfo.EnvironmentVariables['EDUPORTAL_UPDATE_READY_FILE'] = $ready\n"
            + "  $newProcess = [System.Diagnostics.Process]::Start($startInfo)\n"
            + "  if (-not $newProcess) { throw 'Windows did not start the updated launcher.' }\n"
            + "  $deadline = (Get-Date).AddSeconds(30)\n"
            + "  while (-not (Test-Path -LiteralPath $ready -PathType Leaf)) {\n"
            + "    $newProcess.Refresh()\n"
            + "    if ($newProcess.HasExited -and $newProcess.ExitCode -ne 0) { throw ('Updated launcher exited during startup with code ' + $newProcess.ExitCode + '.') }\n"
            + "    if ((Get-Date) -ge $deadline) { throw 'The updated application did not signal that its window opened within 30 seconds.' }\n"
            + "    Start-Sleep -Milliseconds 250\n"
            + "  }\n"
            + "  Write-UpdateLog ('Updated application window opened; launcher process ' + $newProcess.Id + '.')\n"
            + "  Remove-Item -LiteralPath $backup -Force\n"
            + "} catch {\n"
            + "  $failure = $_.Exception.Message\n"
            + "  Write-UpdateLog ('Update/restart failed: ' + $failure)\n"
            + "  try {\n"
            + "    if (Test-Path -LiteralPath $backup -PathType Leaf) {\n"
            + "      if (Test-Path -LiteralPath $target -PathType Leaf) {\n"
            + "        [System.IO.File]::Replace($backup, $target, $failed)\n"
            + "        Remove-Item -LiteralPath $failed -Force -ErrorAction SilentlyContinue\n"
            + "      } else {\n"
            + "        Move-Item -LiteralPath $backup -Destination $target -Force\n"
            + "      }\n"
            + "      Write-UpdateLog 'Restored the previous executable.'\n"
            + "    }\n"
            + "    if (Test-Path -LiteralPath $target -PathType Leaf) {\n"
            + "      $fallback = Start-Process -FilePath $target -WorkingDirectory (Split-Path -Parent $target) -PassThru\n"
            + "      Write-UpdateLog ('Started fallback launcher process ' + $fallback.Id + '.')\n"
            + "    }\n"
            + "  } catch { Write-UpdateLog ('Fallback restart failed: ' + $_.Exception.Message) }\n"
            + "  try {\n"
            + "    Add-Type -AssemblyName System.Windows.Forms\n"
            + "    [System.Windows.Forms.MessageBox]::Show('EduPortal could not finish installing the update and attempted to reopen.'"
            + " + [Environment]::NewLine + [Environment]::NewLine + $failure"
            + " + [Environment]::NewLine + [Environment]::NewLine + 'Details: ' + $log,"
            + " 'EduPortal update', 'OK', 'Error') | Out-Null\n"
            + "  } catch { Write-UpdateLog ('Could not display update error: ' + $_.Exception.Message) }\n"
            + "} finally {\n"
            + "  Remove-Item -LiteralPath $replacement -Force -ErrorAction SilentlyContinue\n"
            + "  Remove-Item -LiteralPath $failed -Force -ErrorAction SilentlyContinue\n"
            + "  Remove-Item -LiteralPath $ready -Force -ErrorAction SilentlyContinue\n"
            + "  if (-not (Test-Path -LiteralPath $backup)) { Remove-Item -LiteralPath $staged -Force -ErrorAction SilentlyContinue }\n"
            + "  Remove-Item -LiteralPath $MyInvocation.MyCommand.Path -Force -ErrorAction SilentlyContinue\n"
            + "}\n";
        File.WriteAllText(helperPath, script);
        try
        {
            ProcessStartInfo startInfo = new ProcessStartInfo();
            startInfo.FileName = "powershell.exe";
            startInfo.Arguments = "-WindowStyle Hidden -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "
                + QuoteArgument(helperPath);
            startInfo.UseShellExecute = false;
            startInfo.CreateNoWindow = true;
            if (Process.Start(startInfo) == null)
            {
                throw new InvalidOperationException("The update installer could not be started.");
            }
        }
        catch
        {
            File.Delete(helperPath);
            throw;
        }
    }

    private static string PowerShellQuote(string value)
    {
        return "'" + value.Replace("'", "''") + "'";
    }

    private static string QuoteArgument(string value)
    {
        StringBuilder quoted = new StringBuilder();
        quoted.Append('"');
        int backslashes = 0;
        foreach (char character in value)
        {
            if (character == '\\')
            {
                backslashes++;
                continue;
            }
            if (character == '"')
            {
                quoted.Append('\\', backslashes * 2 + 1);
                quoted.Append('"');
            }
            else
            {
                quoted.Append('\\', backslashes);
                quoted.Append(character);
            }
            backslashes = 0;
        }
        quoted.Append('\\', backslashes * 2);
        quoted.Append('"');
        return quoted.ToString();
    }

    private static void ShowCleanupWarning(string appDirectory, Exception exception)
    {
        MessageBox.Show(
            "EduPortal closed, but temporary runtime files could not be removed from:\n"
                + appDirectory + "\n\n" + exception.Message,
            "EduPortal cleanup",
            MessageBoxButtons.OK,
            MessageBoxIcon.Warning);
    }
}
