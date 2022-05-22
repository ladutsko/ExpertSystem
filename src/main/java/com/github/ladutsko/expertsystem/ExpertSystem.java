/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 George Ladutsko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.ladutsko.expertsystem;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.IntStream;

import static com.github.ladutsko.expertsystem.MathEx.compare;
import static java.nio.file.Files.newBufferedReader;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * @author <a href="mailto:ladutsko@gmail.com">George Ladutsko</a>
 */
public class ExpertSystem {

    private static final Logger logger = getLogger();

    private final List<Hypothesis> hypotheses;
    private final Map<Integer, Evidence> evidences;

    public ExpertSystem() {
        hypotheses = loadHypotheses();
        evidences = loadEvidences();
    }

    public void start() {
        System.out.println("Инициализация ...");
        double[] probabilities = hypotheses
            .stream()
            .mapToDouble(Hypothesis::getP)
            .toArray();
        int[] questions = hypotheses
            .stream()
            .map(Hypothesis::getHypothesisProbabilities)
            .mapToInt(Map::size)
            .toArray();

        Map<Integer, Long> ruleValue = hypotheses
            .stream()
            .parallel()
            .flatMap(h -> h.getHypothesisProbabilities().keySet().stream())
            .collect(groupingBy(identity(), counting()));
        Map<Integer, Set<Integer>> evidenceHypotheses = IntStream
            .range(0, hypotheses.size())
            .parallel()
            .boxed()
            .flatMap(index -> hypotheses.get(index).getHypothesisProbabilities().keySet().stream().map(evidenceId -> Pair.of(evidenceId, index)))
            .collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, toSet())));

        double[] maxi = new double[hypotheses.size()];
        double[] mini = new double[hypotheses.size()];

        for (;;) {
            long rv = 0L;
            int bestVar = 0;

            for (Map.Entry<Integer, Long> entry : ruleValue.entrySet()) {
                if (entry.getValue() > rv) {
                    rv = entry.getValue();
                    bestVar = entry.getKey();
                }
                entry.setValue(0L);
            }

            if (0 == bestVar) {
                System.out.println("Симптомов больше нет");
                return;
            }

            int response = ask(evidences.get(bestVar));

            for (int i : evidenceHypotheses.remove(bestVar)) {
                if (0 == questions[i]) {
                    continue;
                }

                Hypothesis.HypothesisProbability hp = hypotheses.get(i).getHypothesisProbabilities().get(bestVar);

                questions[i]--;

                double p = probabilities[i];
                double py = hp.getPy();
                double pn = hp.getPn();
                double pe = p * py + (1.0 - p) * pn;

                if (0 < response) {
                    probabilities[i] = p * (1.0 + (py / pe - 1.0) * response / 5.0);
                } else {
                    probabilities[i] = p * (1.0 + (py - (1.0 - py) * pe / (1.0 - pe)) * response / 5.0);
                }

                if (0 == compare(0.0, probabilities[i]) || 0 == compare(1.0, probabilities[i])) {
                    questions[i] = 0;
                }
            }

            double maxOfMin = 0.0;
            int best = 0;

            for (int i = 0; i < hypotheses.size(); i++) {
                double p = probabilities[i];

                double a1 = 1.0;
                double a2 = 1.0;
                double a3 = 1.0;
                double a4 = 1.0;

                Hypothesis hypothesis = hypotheses.get(i);

                if (0 < questions[i]) {
                    for (Hypothesis.HypothesisProbability hp : hypothesis.getHypothesisProbabilities().values()) {
                        int evidenceId = hp.getEvidenceId();

                        if (!evidenceHypotheses.containsKey(evidenceId)) {
                            continue;
                        }

                        double py = hp.getPy();
                        double pn = hp.getPn();

                        if (pn > py) {
                            py = 1.0 - py;
                            pn = 1.0 - pn;
                        }

                        ruleValue.put(evidenceId, ruleValue.get(evidenceId) + 1L);

                        a1 = a1 * py;
                        a2 = a2 * pn;
                        a3 = a3 * (1.0 - py);
                        a4 = a4 * (1.0 - pn);
                    }
                }

                maxi[i] = p * a1 / (p * a1 + (1.0 - p) * a2);
                mini[i] = p * a3 / (p * a3 + (1.0 - p) * a4);

                if (maxi[i] < hypothesis.getP()) {
                    questions[i] = 0;
                    System.out.println("Можно исключить: " + hypothesis.getName());
                }

                if (mini[i] > maxOfMin) {
                    maxOfMin = mini[i];
                    best = i;
                }
            }

            for (int i = 0; i < hypotheses.size(); i++) {
                if (mini[best] <= maxi[i] && i != best) {
                    maxOfMin = 0.0;
                    break;
                }
            }

            if (0 == compare(0.0, maxOfMin)) {
                continue;
            }

            System.out.println("Наиболее вероятный исход: " + hypotheses.get(best).getName());
            System.out.println("с вероятностью: " + probabilities[best]);
            return;
        }
    }

    private int ask(Evidence evidence) {
        Scanner in = new Scanner(System.in);
        System.out.println("Вопрос: " + evidence.getQuestion());
        for (;;) {
            System.out.print("Ответ по шкале от -5 (Нет) до +5 (Да) ");
            try {
                int response = in.nextInt();
                if (-5 <= response && 5 >= response) {
                    return response;
                }
            } catch (Exception e) {
                // nothing
                e.printStackTrace();
            }
        }
    }

    private Map<Integer, Evidence> loadEvidences() {
        System.out.println("Загрузка списка симптомов ...");
        try (BufferedReader reader = newBufferedReader(Path.of("data", "evidence.csv"))) {
            List<Evidence> evidences = new EvidenceReader().read(reader);
            logger.debug(evidences);
            return evidences.stream().parallel().collect(toMap(Evidence::getId, identity()));
        } catch (IOException e) {
            throw new UncheckedIOException("Evidences parsing failed", e);
        }
    }

    private List<Hypothesis> loadHypotheses() {
        System.out.println("Загрузка списка болезней ...");
        try (BufferedReader reader = newBufferedReader(Path.of("data", "hypothesis.csv"))) {
            List<Hypothesis> hypotheses = new HypothesisReader().read(reader);
            logger.debug(hypotheses);
            return hypotheses;
        } catch (IOException e) {
            throw new UncheckedIOException("Hypotheses parsing failed", e);
        }
    }
}
