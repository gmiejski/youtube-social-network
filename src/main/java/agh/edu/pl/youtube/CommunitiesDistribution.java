package agh.edu.pl.youtube;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by grzegorz.miejski on 11/01/16.
 */
public class CommunitiesDistribution {


    public static void main(String[] args) throws IOException {

        List<String> strings = Files.readAllLines(Paths.get("/Users/grzegorz.miejski/home/workspaces/aaaaStudia/Semestr_IX/Analiza_danych/youtube-social-network/src/main/resources/communitiesDistribution.txt"));

        Stream<Integer> integerStream = strings.stream().map(s -> s.split(":")[1]).map(s -> s.trim().split(" ").length);

        Map<Integer, Long> collect = integerStream.collect(Collectors.groupingBy(x -> x, Collectors.counting()));
        System.out.println();
    }
}
