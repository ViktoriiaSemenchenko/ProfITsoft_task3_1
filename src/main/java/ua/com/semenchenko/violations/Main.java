package ua.com.semenchenko.violations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Read data from all files, calculate and display violations into a file.
 * The output file must contain total fines for each type of violations for all years,
 * sorted by the amount (initially the highest amount of the fine).
 * <p>
 * Modify the task so that different files from a folder are downloaded asynchronously using a thread pool,
 * but the overall statistics are generated in the same way.
 * Use CompletableFuture and ExecutorService.
 * Compare the performance of the program when parallelization is not used, when 2 threads are used, 4 and 8.
 *
 * @author Semenchenko V.
 */


public class Main {
    public static void main(String[] args) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        //violations - stores Violation type objects
        List<Violation> violations = Collections.synchronizedList(new ArrayList<>());
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        //typeAndFineAmount - stores Map where key is type of violation and value is fine amount
        Map<String, Double> typeAndFineAmount = new LinkedHashMap<>();

        //sortedTypeAndFineAmount - stores sorted by value Map where key is type of violation and value is fine amount
        Map<String, Double> sortedTypeAndFineAmount = new LinkedHashMap<>();

        //NamesOfFilesWithViolations - stores name of json file
        List<String> NamesOfFilesWithViolations = getNamesOfFilesWithViolations();

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("./src/main/resources/output.xml"))) {

            long start = System.currentTimeMillis();

            //read files in thread pool
            CompletableFuture<Void> future = CompletableFuture.allOf(NamesOfFilesWithViolations.stream()
                    .map(jsonFile -> CompletableFuture
                            .supplyAsync(() -> readFilesOfViolation(jsonFile, objectMapper), executorService)
                            .thenAccept(violations::addAll)).toArray(CompletableFuture[]::new));
            future.join();
            executorService.shutdown();

            //get time of threads work
            System.out.printf("Used time %s ms%n", System.currentTimeMillis() - start);


            getTypeAndFineAmount(violations, typeAndFineAmount);

            typeAndFineAmount = sortTypeAndFineAmount(typeAndFineAmount, sortedTypeAndFineAmount);

            writeStatisticsOfViolationsToXMLFile(typeAndFineAmount, writer);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * getNamesOfFilesWithViolations
     * look for .json files and write their names to the list
     *
     * @return
     * @throws IOException
     */
    public static List<String> getNamesOfFilesWithViolations() throws IOException {
        try (Stream<Path> walk = Files.walk(Paths.get("src/main/resources"), 1)) {
            return walk
                    .filter(p -> !Files.isDirectory(p))
                    .map(Path::getFileName)
                    .map(p -> p.toString().toLowerCase())
                    .filter(f -> f.endsWith("json"))
                    .collect(Collectors.toList());
        }
    }

    /**
     * readFilesOfViolation
     * read .json files
     *
     * @param fileName
     * @param objectMapper
     * @return
     */

    public static List<Violation> readFilesOfViolation(String fileName, ObjectMapper objectMapper) {
        try (BufferedReader br = new BufferedReader(new FileReader("./src/main/resources/" + fileName))) {
            return objectMapper.readValue(br, new TypeReference<List<Violation>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * getTypeAndFineAmount
     * get a Map with type and fine amount of violations
     *
     * @param violations
     * @param typeAndFineAmount
     */

    public static void getTypeAndFineAmount(List<Violation> violations, Map<String, Double> typeAndFineAmount) {
        for (Violation violation : violations) {

            String key = violation.getType();
            double value = violation.getFine_amount();
            if (typeAndFineAmount.containsKey(key)) {
                typeAndFineAmount.put(key, typeAndFineAmount.get(key) + value);
            } else {
                typeAndFineAmount.put(key, value);
            }
        }
    }

    /**
     * sortTypeAndFineAmount
     *
     * @param typeAndFineAmount
     * @param sortedTypeAndFineAmount
     * @return
     */

    public static Map<String, Double> sortTypeAndFineAmount(Map<String, Double> typeAndFineAmount, Map<String, Double> sortedTypeAndFineAmount) {
        typeAndFineAmount.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry -> sortedTypeAndFineAmount.put(entry.getKey(), entry.getValue()));

        return sortedTypeAndFineAmount;
    }

    /**
     * writeStatisticsOfViolationsToXMLFile
     *
     * @param typeAndFineAmount
     * @param writer
     * @throws IOException
     */

    public static void writeStatisticsOfViolationsToXMLFile(Map<String, Double> typeAndFineAmount, BufferedWriter writer) throws IOException {
        writer.write("<violations>\n");

        typeAndFineAmount.forEach((key, value) -> {
            try {
                writer.write("    " + "<violation type=\"" + key + "\" fine_amount=\"" + value + "\" />\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        writer.write("</violations>\n");
    }
}
