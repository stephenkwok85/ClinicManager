package com.example.demo;

import javafx.application.Platform;
import model.project1.*;
import model.util.Doctor;
import model.util.Technician;
import model.util.Person;
import model.project1.List;
import model.project1.Appointment;
import model.project1.Patient;
import model.project1.Profile;
import model.project1.Timeslot;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.Scanner;

public class ClinicManagerController {

    @FXML
    private TextField patient_first_name;

    @FXML
    private TextField patient_last_name;

    @FXML
    private DatePicker appointment_date;

    @FXML
    private DatePicker date_of_birth;

    @FXML
    private ComboBox<String> timeslot_selection;

    @FXML
    private ComboBox<String> provider_selection;

    @FXML
    private TextArea status_messages;

    @FXML
    private Button schedule_button;

    @FXML
    private Label welcomeText;

    @FXML
    private ComboBox<String> display_selector;

    @FXML
    private ComboBox<String> appointment_type;

    private ObservableList<String> providerList;
    private static final String PROVIDERS_FILE_PATH = "providers.txt";
    private List<Appointment> appointmentList;

    @FXML
    public void initialize() {
        providerList = FXCollections.observableArrayList();
        loadProviders();

        display_selector.getItems().addAll("Display office appointments", "Display imaging appointments", "Display all appointments by date", "Display all appointments by patient", "Display all appointments by county", "Display billing statements for all patients", "Display all credit amounts for providers");
        appointment_type.getItems().addAll("Office", "Imaging");

        appointment_type.setOnAction(e -> {
            String selectedType = appointment_type.getValue();
            System.out.println("Selected: " + selectedType);
        });

        display_selector.setOnAction(e -> {
            String selectedOption = display_selector.getValue();
            System.out.println("Selected: " + selectedOption);
        });

        appointmentList = new List<>();
        initializeTimeSlots();
    }

    private void loadProviders() {
        try (Scanner scanner = new Scanner(new File(PROVIDERS_FILE_PATH))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    Provider provider = parseProvider(line);
                    if (provider != null) {
                        providerList.add(provider.getProfile().getFname() + " " + provider.getProfile().getLname()
                                + " (" + provider.getLocation().name() + ")");
                    }
                }
            }
            provider_selection.setItems(providerList);
            displayProviders();
        } catch (FileNotFoundException e) {
            appendMessage("Error: " + PROVIDERS_FILE_PATH + " Cannot find providers.txt!");
        }
    }

    private Provider parseProvider(String line) {
        String[] tokens = line.split("\\s+");

        if (tokens.length < 6) {
            status_messages.appendText("Error: Invalid provider data format.\n");
            return null;
        }

        String providerType = tokens[0].toUpperCase();
        String firstName = tokens[1];
        String lastName = tokens[2];
        String[] dateParts = tokens[3].split("/");

        if (dateParts.length != 3) {
            status_messages.appendText("Error: Invalid date format.\n");
            return null;
        }

        Date dateOfBirth = new Date(
                Integer.parseInt(dateParts[2]),
                Integer.parseInt(dateParts[0]),
                Integer.parseInt(dateParts[1]));

        Location location = Location.valueOf(tokens[4].toUpperCase());

        if (providerType.equals("D")) {
            if (tokens.length < 7) {
                status_messages.appendText("Error: Invalid doctor data format.\n");
                return null;
            }
            return new Doctor(new Profile(firstName, lastName, dateOfBirth), location,
                    Specialty.valueOf(tokens[5].toUpperCase()), tokens[6]);
        } else if (providerType.equals("T")) {
            if (tokens.length < 6) {
                status_messages.appendText("Error: Invalid technician data format.\n");
                return null;
            }
            return new Technician(new Profile(firstName, lastName, dateOfBirth), location,
                    Integer.parseInt(tokens[5]));
        }
        return null;
    }

    private void displayProviders() {
        StringBuilder sb = new StringBuilder("Providers loaded:\n");
        for (String provider : providerList) {
            sb.append(provider).append("\n");
        }
        appendMessage(sb.toString());
    }

    private void initializeTimeSlots() {
        ObservableList<String> timeSlots = FXCollections.observableArrayList();
        addTimeSlots(timeSlots, 1, 6); // Morning slots
        addTimeSlots(timeSlots, 7, 12); // Afternoon slots
        timeslot_selection.setItems(timeSlots);
    }

    private void addTimeSlots(ObservableList<String> timeSlots, int startSlot, int endSlot) {
        for (int i = startSlot; i <= endSlot; i++) {
            Timeslot slot = Timeslot.fromString(Integer.toString(i));
            timeSlots.add(slot.toString());
        }
    }

    @FXML
    private void handleSchedule(javafx.event.ActionEvent event) {
        String firstName = patient_first_name.getText();
        String lastName = patient_last_name.getText();
        LocalDate appointmentDate = appointment_date.getValue();
        String timeslotStr = timeslot_selection.getValue();
        String providerName = provider_selection.getValue();
        LocalDate dob = date_of_birth.getValue();

        System.out.println(firstName + " " + lastName + " " + appointmentDate + " " + timeslotStr + " " + providerName + " " + dob);

        if (firstName == null || firstName.isEmpty() || lastName == null || lastName.isEmpty() ||
                appointmentDate == null || timeslotStr == null || providerName == null || dob == null) {
            appendMessage("Fill all fields");
            System.out.println("Empty field exists");
            return;
        }

        if (isDuplicateAppointment(firstName, lastName, dob, appointmentDate, timeslotStr)) {
            appendMessage("Appointment already exists.");
            System.out.println("Appointment already exists.");
            return;
        }

        try {
            createNewAppointment(firstName, lastName, dob, appointmentDate, timeslotStr, providerName);
            Platform.runLater(() -> {
                appendMessage(firstName + " " + appointmentDate + " Success.");
            });
            System.out.println("Success");
            clearFields();
        } catch (Exception e) {
            appendMessage("Error.");
            System.out.println("Error");
        }
        System.out.println("handleSchedule Done");
    }

    private boolean isDuplicateAppointment(String firstName, String lastName, LocalDate dob, LocalDate appointmentDate, String timeslot) {
        for (int i = 0; i < appointmentList.size(); i++) {
            Appointment appointment = appointmentList.get(i);
            Person person = appointment.getPatient();

            if (person instanceof Patient) {
                Patient patient = (Patient) person;

                if (appointment.getDate().equals(appointmentDate) &&
                        appointment.getTimeslot().toString().equals(timeslot) &&
                        patient.getProfile().getFname().equalsIgnoreCase(firstName) &&
                        patient.getProfile().getLname().equalsIgnoreCase(lastName) &&
                        patient.getProfile().getDob().equals(dob)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void createNewAppointment(String firstName, String lastName, LocalDate dob, LocalDate appointmentDate, String timeslot, String provider) {
        try {
            Date dateOfBirth = new Date(dob.getYear(), dob.getMonthValue(), dob.getDayOfMonth());
            Date appointmentDateConverted = new Date(appointmentDate.getYear(), appointmentDate.getMonthValue(), appointmentDate.getDayOfMonth());
            Patient newPatient = new Patient(new Profile(firstName, lastName, dateOfBirth));

            Doctor selectedDoctor = findDoctorByName(provider);
            if (selectedDoctor == null) {
                appendMessage("Doctor does not exist.");
                return;
            }

            Appointment newAppointment = new Appointment(appointmentDateConverted, Timeslot.fromString(timeslot), newPatient, selectedDoctor);
            appointmentList.add(newAppointment);

            appendMessage("Appointment successfully created for " + firstName + " on " + appointmentDate);
            System.out.println("Appointment successfully created for " + firstName + " on " + appointmentDate);
        } catch (Exception e) {
            e.printStackTrace();
            appendMessage("Error while creating appointment.");
        }
    }

    private Doctor findDoctorByName(String providerName) {
        String[] providerParts = providerName.split(" ");
        if (providerParts.length < 2) {
            return null;
        }

        String firstName = providerParts[0];
        String lastName = providerParts[1];

        for (String providerInfo : providerList) {
            String[] providerDetails = providerInfo.split(" ");
            if (providerDetails.length < 2) {
                continue;
            }

            String providerFirstName = providerDetails[0];
            String providerLastName = providerDetails[1];

            if (firstName.equalsIgnoreCase(providerFirstName) && lastName.equalsIgnoreCase(providerLastName)) {
                for (int i = 0; i < providerList.size(); i++) {
                    Provider provider = parseProvider(providerList.get(i));
                    if (provider instanceof Doctor) {
                        return (Doctor) provider;
                    }
                }
            }
        }

        return null;
    }


    private void clearFields() {
        patient_first_name.clear();
        patient_last_name.clear();
        appointment_date.setValue(null);
        timeslot_selection.setValue(null);
        provider_selection.setValue(null);
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> {
            status_messages.appendText(message + "\n");
        });
    }
}
