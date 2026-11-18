package com.eduportal.ui;

import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PhoneNumberField extends HBox {

    private static final List<CountryCode> COUNTRIES = List.of(
            new CountryCode("Bangladesh", "+880"), new CountryCode("United States", "+1"),
            new CountryCode("Canada", "+1"), new CountryCode("United Kingdom", "+44"),
            new CountryCode("Australia", "+61"), new CountryCode("India", "+91"),
            new CountryCode("Pakistan", "+92"), new CountryCode("Nepal", "+977"),
            new CountryCode("Sri Lanka", "+94"), new CountryCode("Bhutan", "+975"),
            new CountryCode("China", "+86"), new CountryCode("Japan", "+81"),
            new CountryCode("South Korea", "+82"), new CountryCode("Singapore", "+65"),
            new CountryCode("Malaysia", "+60"), new CountryCode("Indonesia", "+62"),
            new CountryCode("Philippines", "+63"), new CountryCode("Thailand", "+66"),
            new CountryCode("Vietnam", "+84"), new CountryCode("United Arab Emirates", "+971"),
            new CountryCode("Saudi Arabia", "+966"), new CountryCode("Qatar", "+974"),
            new CountryCode("Kuwait", "+965"), new CountryCode("Turkey", "+90"),
            new CountryCode("Germany", "+49"), new CountryCode("France", "+33"),
            new CountryCode("Italy", "+39"), new CountryCode("Spain", "+34"),
            new CountryCode("Netherlands", "+31"), new CountryCode("Switzerland", "+41"),
            new CountryCode("Sweden", "+46"), new CountryCode("Norway", "+47"),
            new CountryCode("Denmark", "+45"), new CountryCode("Ireland", "+353"),
            new CountryCode("New Zealand", "+64"), new CountryCode("South Africa", "+27"),
            new CountryCode("Nigeria", "+234"), new CountryCode("Egypt", "+20"),
            new CountryCode("Brazil", "+55"), new CountryCode("Mexico", "+52"),
            new CountryCode("Argentina", "+54"), new CountryCode("Chile", "+56"),
            new CountryCode("Russia", "+7"), new CountryCode("Ukraine", "+380"),
            new CountryCode("Israel", "+972"), new CountryCode("Jordan", "+962"),
            new CountryCode("Afghanistan", "+93"), new CountryCode("Myanmar", "+95"),
            new CountryCode("Cambodia", "+855"), new CountryCode("Hong Kong", "+852"),
            new CountryCode("Taiwan", "+886"), new CountryCode("Kenya", "+254"),
            new CountryCode("Ghana", "+233"), new CountryCode("Ethiopia", "+251")
    );

    private final ComboBox<CountryCode> countrySelector = new ComboBox<>();
    private final TextField nationalNumber = new TextField();

    public PhoneNumberField(String storedNumber) {
        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);
        List<CountryCode> countryOptions = new ArrayList<>(COUNTRIES);
        countryOptions.sort(Comparator.comparing(country -> country.name));
        countrySelector.getItems().setAll(countryOptions);
        countrySelector.setPrefWidth(230);
        countrySelector.setValue(COUNTRIES.get(0));
        countrySelector.setPromptText("Country code");

        nationalNumber.setPromptText("Number without trunk prefix");
        nationalNumber.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            return text.matches("\\d{0,15}") ? change : null;
        }));
        HBox.setHgrow(nationalNumber, javafx.scene.layout.Priority.ALWAYS);

        if (storedNumber != null && !storedNumber.trim().isEmpty()) {
            loadStoredNumber(storedNumber.trim());
        }
        getChildren().addAll(countrySelector, nationalNumber);
    }

    public boolean isValid() {
        CountryCode country = countrySelector.getValue();
        return isValidPhoneNumber(country == null ? null : country.name,
                country == null ? null : country.callingCode, nationalNumber.getText());
    }

    public static boolean isValidPhoneNumber(String countryName, String callingCode, String input) {
        String number = input == null ? "" : input.trim();
        if (number.isEmpty()) return true;
        if (countryName == null || callingCode == null || !number.matches("\\d+")) return false;
        boolean countryCodeMatches = COUNTRIES.stream().anyMatch(country ->
                country.name.equals(countryName) && country.callingCode.equals(callingCode));
        if (!countryCodeMatches) return false;
        int internationalDigits = callingCode.length() - 1 + number.length();
        if (internationalDigits < 7 || internationalDigits > 15) return false;
        switch (countryName) {
            case "Bangladesh": return number.length() == 10 && number.matches("1\\d{9}");
            case "United States":
            case "Canada": return number.length() == 10 && number.matches("[2-9]\\d{2}[2-9]\\d{6}");
            case "United Kingdom": return number.length() == 10 && number.matches("[1-9]\\d{9}");
            case "Australia": return number.length() == 9 && number.matches("[23478]\\d{8}");
            case "India": return number.length() == 10 && number.matches("[6-9]\\d{9}");
            case "Pakistan": return number.length() == 10 && number.matches("[3-9]\\d{9}");
            case "Nepal": return number.length() == 10 && number.matches("[9]\\d{9}");
            case "Sri Lanka": return number.length() == 9 && number.matches("[1-9]\\d{8}");
            case "Bhutan": return number.length() == 8 && number.matches("[17]\\d{7}");
            case "China": return number.length() == 11 && number.matches("1\\d{10}");
            case "Japan": return number.length() == 10 && number.matches("[1-9]\\d{9}");
            case "South Korea": return number.length() == 10 || number.length() == 11;
            case "Singapore": return number.length() == 8 && number.matches("[3689]\\d{7}");
            case "Malaysia": return number.length() >= 9 && number.length() <= 10 && number.matches("[1-9]\\d+");
            case "Indonesia": return number.length() >= 9 && number.length() <= 12 && number.startsWith("8");
            case "Philippines": return number.length() == 10 && number.startsWith("9");
            case "Thailand": return number.length() == 9 && number.matches("[689]\\d{8}");
            case "Vietnam": return number.length() == 9 && number.matches("[3-9]\\d{8}");
            case "United Arab Emirates": return number.length() == 9 && number.matches("[2-9]\\d{8}");
            case "Saudi Arabia": return number.length() == 9 && number.matches("[5]\\d{8}");
            case "Qatar": return number.length() == 8 && number.matches("[3-7]\\d{7}");
            case "Kuwait": return number.length() == 8 && number.matches("[24569]\\d{7}");
            case "Turkey": return number.length() == 10 && number.matches("[1-9]\\d{9}");
            case "Germany": return number.length() >= 7 && number.length() <= 12 && number.matches("[1-9]\\d+");
            case "France": return number.length() == 9 && number.matches("[1-9]\\d{8}");
            case "Italy": return number.length() >= 9 && number.length() <= 10 && number.matches("[0-9]\\d+");
            case "Spain": return number.length() == 9 && number.matches("[6-9]\\d{8}");
            case "Netherlands": return number.length() == 9 && number.matches("[1-9]\\d{8}");
            case "Switzerland": return number.length() == 9 && number.matches("[1-9]\\d{8}");
            case "Sweden": return number.length() >= 7 && number.length() <= 10 && number.matches("[1-9]\\d+");
            case "Norway": return number.length() == 8 && number.matches("[2-9]\\d{7}");
            case "Denmark": return number.length() == 8 && number.matches("[2-9]\\d{7}");
            case "Ireland": return number.length() == 9 && number.matches("[1-9]\\d{8}");
            case "New Zealand": return number.length() >= 8 && number.length() <= 10 && number.matches("[1-9]\\d+");
            case "South Africa": return number.length() == 9 && number.matches("[1-8]\\d{8}");
            case "Nigeria": return number.length() == 10 && number.matches("[789]\\d{9}");
            case "Egypt": return number.length() == 10 && number.startsWith("1");
            case "Brazil": return number.length() == 10 || number.length() == 11;
            case "Mexico": return number.length() == 10 && number.matches("[1-9]\\d{9}");
            case "Argentina": return number.length() == 10 && number.matches("[1-9]\\d{9}");
            case "Chile": return number.length() == 9 && number.matches("[2-9]\\d{8}");
            case "Russia": return number.length() == 10 && number.matches("[3489]\\d{9}");
            case "Ukraine": return number.length() == 9 && number.matches("[3-9]\\d{8}");
            case "Israel": return number.length() == 9 && number.matches("[2-9]\\d{8}");
            case "Jordan": return number.length() == 9 && number.matches("[1-9]\\d{8}");
            case "Afghanistan": return number.length() == 9 && number.startsWith("7");
            case "Myanmar": return number.length() >= 7 && number.length() <= 10 && number.startsWith("9");
            case "Cambodia": return number.length() >= 8 && number.length() <= 9 && number.matches("[1-9]\\d+");
            case "Hong Kong": return number.length() == 8 && number.matches("[2-9]\\d{7}");
            case "Taiwan": return number.length() == 9 && number.matches("[1-9]\\d{8}");
            case "Kenya": return number.length() == 9 && number.matches("[17]\\d{8}");
            case "Ghana": return number.length() == 9 && number.matches("[235]\\d{8}");
            case "Ethiopia": return number.length() == 9 && number.startsWith("9");
            default: return number.length() >= 7 && number.length() <= 12 && number.matches("[1-9]\\d+");
        }
    }

    public String getInternationalNumber() {
        String number = nationalNumber.getText().trim();
        if (number.isEmpty()) return null;
        return countrySelector.getValue().callingCode + number;
    }

    private void loadStoredNumber(String storedNumber) {
        String digits = storedNumber.replaceAll("\\D", "");
        List<CountryCode> matches = new ArrayList<>(COUNTRIES);
        matches.sort(Comparator.comparingInt((CountryCode c) -> c.callingCode.length()).reversed());
        for (CountryCode country : matches) {
            String prefix = country.callingCode.substring(1);
            if (digits.startsWith(prefix) && digits.length() - prefix.length() >= 7
                    && digits.length() - prefix.length() <= 15) {
                countrySelector.setValue(country);
                nationalNumber.setText(digits.substring(prefix.length()));
                return;
            }
        }
        nationalNumber.setText(digits);
    }

    private static final class CountryCode {
        private final String name;
        private final String callingCode;

        private CountryCode(String name, String callingCode) {
            this.name = name;
            this.callingCode = callingCode;
        }

        @Override
        public String toString() {
            return name + " (" + callingCode + ")";
        }
    }
}
