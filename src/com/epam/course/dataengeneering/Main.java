package com.epam.course.dataengeneering;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static MessageDigest messageDigest;

    static {
        try {
            messageDigest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("cannot initialize SHA-512 hash function", e);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Argument should be specified");
        }

        final Path root = Paths.get(args[0]);
        if (Files.exists(root) && Files.isDirectory(root)) {
            final Map<String, List<String>> duplicates = new HashMap<>();

            try {
                Files.walk(root)
                        .filter(Files::isRegularFile)
                        .forEach(path -> findDuplicates(duplicates, path));

                for (List<String> files : duplicates.values()) {
                    if (files.size() > 1) {
                        final Path target = Paths.get(files.get(0));
                        files.stream().skip(1).forEach(duplicate -> createHardLink(target, duplicate));
                    }
                }
            } catch (IOException e) {
                System.out.printf("Error while trying to walk file tree for %s", root);
            }
        } else {
            System.out.printf("Directory does not exist: %s", root);
        }
    }

    private static void createHardLink(final Path target, final String duplicate) {
        try {
            final Path link = Paths.get(duplicate);
            if (Files.exists(link)) {
                Files.delete(link);
            }
            Files.createLink(link, target);
        } catch (IOException e) {
            System.out.printf("Cannot create hard link for  %s", target);
        }
    }

    private static void findDuplicates(final Map<String, List<String>> duplicates, final Path filePath) {
        final byte[] fileData = readFileData(filePath.toFile());
        final String hash = new BigInteger(1, messageDigest.digest(fileData)).toString(16);
        List<String> list = duplicates.get(hash);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(filePath.toFile().getAbsolutePath());
        duplicates.put(hash, list);
    }

    private static byte[] readFileData(final File file) {
        byte[] fileData;
        try (final FileInputStream fi = new FileInputStream(file)) {
            fileData = new byte[(int) file.length()];
            fi.read(fileData);
        } catch (IOException e) {
            throw new RuntimeException("cannot read file " + file.getAbsolutePath(), e);
        }
        return fileData;
    }
}
