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

import java.util.Map;

/**
 * @author <a href="mailto:ladutsko@gmail.com">George Ladutsko</a>
 */
public class Hypothesis extends Model {

    private String name;
    private double p;
    private Map<Integer, HypothesisProbability> hypothesisProbabilities;

    public String getName() {
        return name;
    }

    public Hypothesis setName(String name) {
        this.name = name;
        return this;
    }

    public double getP() {
        return p;
    }

    public Hypothesis setP(double p) {
        this.p = p;
        return this;
    }

    public Map<Integer, HypothesisProbability> getHypothesisProbabilities() {
        return hypothesisProbabilities;
    }

    public Hypothesis setHypothesisProbabilities(Map<Integer, HypothesisProbability> hypothesisProbabilities) {
        this.hypothesisProbabilities = hypothesisProbabilities;
        return this;
    }

    public static class HypothesisProbability extends Model {

        private final Hypothesis parent;

        private int evidenceId;
        private double py;
        private double pn;

        public HypothesisProbability(Hypothesis parent) {
            this.parent = parent;
        }

        public Hypothesis getParent() {
            return parent;
        }

        public int getEvidenceId() {
            return evidenceId;
        }

        public HypothesisProbability setEvidenceId(int evidenceId) {
            this.evidenceId = evidenceId;
            return this;
        }

        public double getPy() {
            return py;
        }

        public HypothesisProbability setPy(double py) {
            this.py = py;
            return this;
        }

        public double getPn() {
            return pn;
        }

        public HypothesisProbability setPn(double pn) {
            this.pn = pn;
            return this;
        }
    }
}
