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

import com.github.ladutsko.expertsystem.Hypothesis.HypothesisProbability;
import com.opencsv.CSVReader;
import com.opencsv.bean.MappingStrategy;
import com.opencsv.exceptions.CsvBadConverterException;
import com.opencsv.exceptions.CsvBeanIntrospectionException;
import com.opencsv.exceptions.CsvFieldAssignmentException;
import com.opencsv.exceptions.CsvValidationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:ladutsko@gmail.com">George Ladutsko</a>
 */
public class HypothesisMappingStrategy implements MappingStrategy<Hypothesis> {

    static final int NAME_INDEX                      = 0;
    static final int P_INDEX                         = 1;
    static final int SYMPTOM_PROBABILITY_FIRST_INDEX = 2;

    static final String END = "999";

    @Override
    public void captureHeader(CSVReader reader) {
        // nothing
    }

    @Override
    public String[] generateHeader(Hypothesis bean) {
        return new String[0];
    }

    @Override
    public Hypothesis populateNewBean(String[] line) throws CsvBeanIntrospectionException, CsvFieldAssignmentException {
        try {
            Hypothesis hypothesis = new Hypothesis()
                .setName(line[NAME_INDEX])
                .setP(Double.parseDouble(line[P_INDEX]));

            Map<Integer, HypothesisProbability> symptomProbabilities = new LinkedHashMap<>();

            for(int i = SYMPTOM_PROBABILITY_FIRST_INDEX; ; i += 3) {
                String tmp = line[i];
                if (END.equals(tmp)) {
                    break;
                }

                int evidenceId = Integer.parseInt(tmp);
                symptomProbabilities.put(evidenceId, new HypothesisProbability(hypothesis)
                    .setEvidenceId(evidenceId)
                    .setPy(Double.parseDouble(line[i + 1]))
                    .setPn(Double.parseDouble(line[i + 2])));
            }

            hypothesis.setHypothesisProbabilities(symptomProbabilities);

            return hypothesis;
        } catch (Exception e) {
            CsvValidationException csvValidationException = new CsvValidationException(e.getMessage());
            csvValidationException.addSuppressed(e);
            throw csvValidationException;
        }
    }

    @Override
    public void setType(Class<? extends Hypothesis> type) throws CsvBadConverterException {
        // nothing
    }

    @Override
    public String[] transmuteBean(Hypothesis bean) {
        List<String> result = new ArrayList<>(2 + bean.getHypothesisProbabilities().size() * 3 + 1);

        result.add(bean.getName());
        result.add(Double.toString(bean.getP()));

        bean.getHypothesisProbabilities().values().forEach(entry -> {
            result.add(Integer.toString(entry.getEvidenceId()));
            result.add(Double.toString(entry.getPy()));
            result.add(Double.toString(entry.getPn()));
        });

        result.add(END);

        return result.toArray(new String[0]);
    }
}
