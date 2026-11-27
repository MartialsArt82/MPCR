package mpc.remote;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

final class SavedDevices {
    static final class Device {
        final String address;
        final String name;

        Device(String address, String name) {
            this.address = address;
            this.name = name;
        }

        String label() {
            return name.isEmpty() || name.equals(address) ? address : name + " · " + address;
        }
    }

    private final Properties entries = new Properties();

    static SavedDevices read(String content) {
        SavedDevices result = new SavedDevices();
        try {
            result.entries.load(new StringReader(content));
        } catch (IOException | IllegalArgumentException e) {
            return new SavedDevices();
        }
        return result;
    }

    List<Device> list() {
        List<Device> devices = new ArrayList<>();
        for (String address : entries.stringPropertyNames()) {
            devices.add(new Device(address, entries.getProperty(address)));
        }
        devices.sort(Comparator.comparing(Device::label, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(device -> device.address));
        return devices;
    }

    SavedDevices with(String address, String name) {
        SavedDevices updated = new SavedDevices();
        updated.entries.putAll(entries);
        updated.entries.setProperty(address.trim(), name.trim());
        return updated;
    }

    String serialize() {
        StringWriter output = new StringWriter();
        try {
            entries.store(output, "MPCR-Screen saved devices");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return output.toString();
    }
}
