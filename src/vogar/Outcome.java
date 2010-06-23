/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vogar;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * An outcome of an action. Some actions may have multiple outcomes. For
 * example, JUnit tests have one outcome for each test method.
 */
final class Outcome {

    private final String outcomeName;
    private final Result result;
    private final String output;
    private final Date date;

    public static final Ordering<Outcome> ORDER_BY_NAME = new Ordering<Outcome>() {
        @Override public int compare(Outcome a, Outcome b) {
            return a.getName().compareTo(b.getName());
        }
    };

    public Outcome(String outcomeName, Result result, List<String> outputLines, Date date) {
        this.outcomeName = outcomeName;
        this.result = result;
        this.output = sanitizeOutputLines(outputLines);
        this.date = date;
    }

    public Outcome(String outcomeName, Result result, List<String> outputLines) {
        this.outcomeName = outcomeName;
        this.result = result;
        this.output = sanitizeOutputLines(outputLines);
        this.date = new Date();
    }

    public Outcome(String outcomeName, Result result, String outputLine, Date date) {
        this.outcomeName = outcomeName;
        this.result = result;
        this.output = sanitizeOutputLine(outputLine);
        this.date = date;
    }

    public Outcome(String outcomeName, Result result, String outputLine) {
        this.outcomeName = outcomeName;
        this.result = result;
        this.output = sanitizeOutputLine(outputLine);
        this.date = new Date();
    }

    public Outcome(String outcomeName, Result result, Throwable throwable) {
        this.outcomeName = outcomeName;
        this.result = result;
        this.output = sanitizeOutputLines(throwableToLines(throwable));
        this.date = new Date();
    }

    private String sanitizeOutputLines(List<String> outputLines) {
        List<String> sanitizedStrings = Lists.newArrayList();
        for (String line : outputLines) {
            sanitizedStrings.add(sanitizeOutputLine(line));
        }
        return Strings.join(sanitizedStrings, "\n");
    }

    private String sanitizeOutputLine(String outputLine) {
        return Strings.xmlSanitize(outputLine.replaceAll("\r\n?", "\n"));
    }

    public Date getDate() {
        return date;
    }

    public String getName() {
        return outcomeName;
    }

    public Result getResult() {
        return result;
    }

    public String getOutput() {
        return output;
    }

    public List<String> getOutputLines() {
        return Arrays.asList(output.split("\n"));
    }

    private static List<String> throwableToLines(Throwable t) {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        t.printStackTrace(out);
        return Arrays.asList(writer.toString().split("\\n"));
    }

    /**
     * Returns the action's suite name, such as java.lang.Integer or
     * java.lang.IntegerTest.
     */
    public String getSuiteName() {
        int split = split(outcomeName);
        return split == -1 ? "defaultpackage" : outcomeName.substring(0, split);
    }

    /**
     * Returns the specific action name, such as BitTwiddle or testBitTwiddle.
     */
    public String getTestName() {
        int split = split(outcomeName);
        return split == -1 ? outcomeName : outcomeName.substring(split + 1);
    }

    private static int split(String name) {
        int lastHash = name.indexOf('#');
        return lastHash == -1 ? name.lastIndexOf('.') : lastHash;
    }

    /**
     * Returns whether the result indicates that the contents of the Outcome are important.
     *
     * For example, for a test skipped because it is unsupported, we don't care about the result.
     */
    private boolean matters() {
        return result != Result.UNSUPPORTED;
    }

    public ResultValue getResultValue(Expectation expectation) {
        if (matters()) {
            return expectation.matches(this) ? ResultValue.OK : ResultValue.FAIL;
        }
        return ResultValue.IGNORE;
    }

    public String getPath() {
        return Strings.join(outcomeName.split("\\.|#"), "/");
    }

    @Override public boolean equals(Object o) {
        if (o instanceof Outcome) {
            Outcome outcome = (Outcome) o;
            return outcomeName.equals(outcome.outcomeName)
                    && result == outcome.result
                    && output.equals(outcome.output);
        }
        return false;
    }

    @Override public int hashCode() {
        int hashCode = 17;
        hashCode = 37 * hashCode + outcomeName.hashCode();
        hashCode  = 37 * hashCode + result.hashCode();
        hashCode = 37 * hashCode + output.hashCode();
        return hashCode;
    }
}
