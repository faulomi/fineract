/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.core.data;

import static java.util.stream.Collectors.toList;

import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.validate.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.quartz.CronExpression;
import org.springframework.util.ObjectUtils;

public class DataValidatorBuilder {

    public static final String VALID_INPUT_SEPERATOR = "_";
    private final List<ApiParameterError> dataValidationErrors;
    private String resource;
    private String parameter;
    private String arrayPart;
    private Integer arrayIndex;
    private Object value;
    private boolean ignoreNullValue = false;

    /**
     * Default constructor used to start a new "validation chain".
     */
    public DataValidatorBuilder() {
        this(new ArrayList<>());
    }

    /**
     * Constructor used to "continue" an existing "validation chain".
     *
     * @param dataValidationErrors
     *            an existing list of {@link ApiParameterError} to add new validation errors to
     */

    public DataValidatorBuilder(final List<ApiParameterError> dataValidationErrors) {
        this.dataValidationErrors = dataValidationErrors;
    }

    public DataValidatorBuilder reset() {
        return new DataValidatorBuilder(this.dataValidationErrors).resource(this.resource);
    }

    public void merge(DataValidatorBuilder other) {
        dataValidationErrors.addAll(other.dataValidationErrors);
    }

    public boolean hasError() {
        return !dataValidationErrors.isEmpty();
    }

    public List<ApiParameterError> getDataValidationErrors() {
        return dataValidationErrors;
    }

    public DataValidatorBuilder resource(final String resource) {
        this.resource = resource;
        return this;
    }

    public DataValidatorBuilder parameter(final String parameter) {
        this.parameter = parameter;
        return this;
    }

    public DataValidatorBuilder parameterAtIndexArray(final String arrayPart, final Integer arrayIndex) {
        this.arrayPart = arrayPart;
        this.arrayIndex = arrayIndex;
        return this;
    }

    public DataValidatorBuilder value(final Object value) {
        this.value = value;
        return this;
    }

    public DataValidatorBuilder ignoreIfNull() {
        this.ignoreNullValue = true;
        return this;
    }

    public DataValidatorBuilder andNotBlank(final String linkedParameterName, final String linkedValue) {
        if (this.value == null && linkedValue == null && this.ignoreNullValue) {
            return this;
        }

        if (StringUtils.isBlank(linkedValue)) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(linkedParameterName).append(".cannot.be.empty.when.").append(this.parameter).append(".is.populated");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(linkedParameterName)
                    .append("` cannot be empty when ").append(this.parameter).append(" is populated.");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), linkedParameterName, linkedValue, this.value);
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public void failWithCode(final String errorCode, final Object... defaultUserMessageArgs) {
        final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                .append(this.parameter).append(".").append(errorCode);
        final StringBuilder defaultEnglishMessage = new StringBuilder("Failed data validation due to: ").append(errorCode).append(".");
        final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(), defaultEnglishMessage.toString(),
                this.parameter, this.value, defaultUserMessageArgs);
        this.dataValidationErrors.add(error);
    }

    public void failWithCodeNoParameterAddedToErrorCode(final String errorCode, final Object... defaultUserMessageArgs) {
        final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".").append(errorCode);
        final StringBuilder defaultEnglishMessage = new StringBuilder("Failed data validation due to: ").append(errorCode).append(".");
        final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(), defaultEnglishMessage.toString(),
                this.parameter, this.value, defaultUserMessageArgs);
        this.dataValidationErrors.add(error);
    }

    public DataValidatorBuilder equalToParameter(final String linkedParameterName, final Object linkedValue) {
        if (this.value == null && linkedValue == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null && !this.value.equals(linkedValue)) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(linkedParameterName).append(".not.equal.to.").append(this.parameter);
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(linkedParameterName)
                    .append("` is not equal to ").append(this.parameter).append(".");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), linkedParameterName, linkedValue, this.value);
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public DataValidatorBuilder notSameAsParameter(final String linkedParameterName, final Object linkedValue) {
        if (this.value == null && linkedValue == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null && this.value.equals(linkedValue)) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(linkedParameterName).append(".same.as.").append(this.parameter);
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(linkedParameterName)
                    .append("` is same as ").append(this.parameter).append(".");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), linkedParameterName, linkedValue, this.value);
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    /*** FIXME: Vishwas, why does this method have a parameter? Seems wrong ***/
    /*
     * This method is not meant for validation, if you have mandatory boolean param and if it has invalid value or value
     * not passed then call this method, this method is always used with input as false
     */
    public DataValidatorBuilder trueOrFalseRequired1(final boolean trueOfFalseFieldProvided) {
        if (!trueOfFalseFieldProvided && !this.ignoreNullValue) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".must.be.true.or.false");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                    .append("` must be set as true or false.");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter);
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public DataValidatorBuilder trueOrFalseRequired(final Object trueOfFalseField) {

        if (trueOfFalseField != null) {
            if (!trueOfFalseField.toString().equalsIgnoreCase("true") && !trueOfFalseField.toString().equalsIgnoreCase("false")) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".must.be.true.or.false");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be set as true or false.");
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter);
                this.dataValidationErrors.add(error);
            }
        }

        return this;
    }

    public DataValidatorBuilder notNull() {
        if (this.value == null && !this.ignoreNullValue) {

            String realParameterName = this.parameter;
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter);
            if (this.arrayIndex != null && StringUtils.isNotBlank(this.arrayPart)) {
                validationErrorCode.append(".").append(this.arrayPart);
                realParameterName = new StringBuilder(this.parameter).append('[').append(this.arrayIndex).append("][")
                        .append(this.arrayPart).append(']').toString();
            }

            validationErrorCode.append(".cannot.be.blank");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(realParameterName)
                    .append("` is mandatory.");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), realParameterName, this.arrayIndex);
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public DataValidatorBuilder notBlank() {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value == null || StringUtils.isBlank(this.value.toString())) {
            String realParameterName = this.parameter;
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter);
            if (this.arrayIndex != null && StringUtils.isNotBlank(this.arrayPart)) {
                validationErrorCode.append(".").append(this.arrayPart);
                realParameterName = new StringBuilder(this.parameter).append('[').append(this.arrayIndex).append("][")
                        .append(this.arrayPart).append(']').toString();
            }

            validationErrorCode.append(".cannot.be.blank");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(realParameterName)
                    .append("` is mandatory.");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), realParameterName, this.arrayIndex);
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public DataValidatorBuilder notExceedingLengthOf(final Integer maxLength) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null && this.value.toString().trim().length() > maxLength) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".exceeds.max.length");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                    .append("` exceeds max length of ").append(maxLength).append(".");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter, maxLength, this.value.toString());
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public DataValidatorBuilder notExceedingListLengthOf(final Integer maxLength) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value instanceof List && ((List<?>) this.value).size() > maxLength) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".exceeds.max.length.allowed");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                    .append("` exceeds allowed max length of ").append(maxLength).append(".");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter);
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public DataValidatorBuilder inMinMaxRange(final Integer min, final Integer max) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final Integer number = Integer.valueOf(this.value.toString());
            if (number < min || number > max) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".is.not.within.expected.range");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be between ").append(min).append(" and ").append(max).append(".");
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, number, min, max);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder isOneOfTheseValues(final Object... values) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        final List<Object> rawValuesList = Arrays.asList(values);

        if (this.value == null || !rawValuesList.contains(this.value)) {
            final List<String> valuesList = Arrays.stream(values).map(Object::toString).collect(toList());
            final String valuesListStr = String.join(", ", valuesList);
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".is.not.one.of.expected.enumerations");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                    .append("` must be one of [ ").append(valuesListStr).append(" ] ").append(".");

            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter, this.value, values);

            this.dataValidationErrors.add(error);
        }

        return this;
    }

    public DataValidatorBuilder isOneOfEnumValues(Class<? extends Enum<?>> e) {
        final List<String> enumValuesList = Arrays.asList(Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new));
        return isOneOfTheseStringValues(enumValuesList);
    }

    public DataValidatorBuilder isOneOfTheseStringValues(final Object... values) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        final List<Object> rawValuesList = Arrays.asList(values);

        if (this.value == null || !rawValuesList.contains(this.value.toString().toLowerCase())) {
            final List<String> valuesList = Arrays.stream(values).map(Object::toString).collect(toList());
            final String valuesListStr = String.join(", ", valuesList);
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".is.not.one.of.expected.enumerations");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                    .append("` must be one of [ ").append(valuesListStr).append(" ] ").append(".");

            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter, this.value, values);

            this.dataValidationErrors.add(error);
        }

        return this;
    }

    public DataValidatorBuilder isOneOfTheseStringValues(final List<String> valuesList) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        List<String> valuesListLowercase = valuesList.stream().map(String::toLowerCase).toList();

        if (this.value == null || !valuesListLowercase.contains(this.value.toString().toLowerCase())) {
            final String valuesListStr = String.join(", ", valuesList);
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".is.not.one.of.expected.enumerations");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                    .append("` must be one of [ ").append(valuesListStr).append(" ] ").append(".");

            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter, this.value, valuesList);

            this.dataValidationErrors.add(error);
        }

        return this;
    }

    public DataValidatorBuilder isNotOneOfTheseValues(final Object... values) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final List<Object> rawValuesList = Arrays.asList(values);

            if (rawValuesList.contains(this.value)) {
                final List<String> valuesList = Arrays.stream(values).map(Object::toString).collect(toList());
                final String valuesListStr = String.join(", ", valuesList);
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".is.one.of.unwanted.enumerations");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must not be any of [ ").append(valuesListStr).append(" ] ").append(".");

                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, this.value, values);

                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder positiveAmount() {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final BigDecimal number = BigDecimal.valueOf(Double.parseDouble(this.value.toString()));
            if (number.compareTo(BigDecimal.ZERO) <= 0) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".not.greater.than.zero");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be greater than 0.");
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, number, 0);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    /*
     * should be used with .notNull() before it
     */
    public DataValidatorBuilder zeroOrPositiveAmount() {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final BigDecimal number = BigDecimal.valueOf(Double.parseDouble(this.value.toString()));
            if (number.compareTo(BigDecimal.ZERO) < 0) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".not.zero.or.greater");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be greater than or equal to 0.");
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, number, 0);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    /*
     * should be used with .notNull() before it
     */
    public DataValidatorBuilder integerZeroOrGreater() {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final Integer number = Integer.valueOf(this.value.toString());
            if (number < 0) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".not.zero.or.greater");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be zero or greater.");
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, number, 0);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    /*
     * should be used with .notNull() before it
     */
    public DataValidatorBuilder integerGreaterThanZero() {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final Integer number = Integer.valueOf(this.value.toString());
            if (number < 1) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".not.greater.than.zero");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be greater than 0.");
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, number, 0);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder integerGreaterThanNumber(Integer number) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final Integer intValue = Integer.valueOf(this.value.toString());
            if (intValue < number + 1) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".not.greater.than.specified.number");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be greater than ").append(number);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, intValue, number);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder integerEqualToOrGreaterThanNumber(Integer number) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final Integer intValue = Integer.valueOf(this.value.toString());
            if (intValue < number) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".not.equal.to.or.greater.than.specified.number");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be equal to or greater than").append(number);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, intValue, number);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder integerSameAsNumber(Integer number) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final Integer intValue = Integer.valueOf(this.value.toString());
            if (!intValue.equals(number)) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".not.equal.to.specified.number");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be same as").append(number);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, intValue, number);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder integerInMultiplesOfNumber(Integer number) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final Integer intValue = Integer.valueOf(this.value.toString());
            if (intValue < number || intValue % number != 0) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".not.in.multiples.of.specified.number");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be multiples of ").append(number);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, intValue, number);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    /*
     * should be used with .notNull() before it
     */
    public DataValidatorBuilder longGreaterThanZero() {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final Long number = Long.valueOf(this.value.toString());
            if (number < 1) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".not.greater.than.zero");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be greater than 0.");
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, number, 0);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    /*
     * should be used with .notNull() before it
     */
    public DataValidatorBuilder longZeroOrGreater() {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final Long number = Long.valueOf(this.value.toString());
            if (number < 0) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".not.equal.or.greater.than.zero");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be equal or greater than 0.");
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, number, 0);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder longGreaterThanNumber(Long number) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final Long longValue = Long.valueOf(this.value.toString());
            if (longValue < number + 1) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".not.greater.than.specified.number");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be greater than ").append(number);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, longValue, number);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder longGreaterThanNumber(String paramName, Long number, int index) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null) {
            final Long longValue = Long.valueOf(this.value.toString());
            if (longValue < number + 1) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".not.greater.than.specified.").append(paramName).append(".at Index.").append(index);
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be greater than ").append(number);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, longValue, number);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder arrayNotEmpty() {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        final Object[] array = (Object[]) this.value;
        if (ObjectUtils.isEmpty(array)) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".cannot.be.empty");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                    .append("` cannot be empty. You must select at least one.");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter);
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public DataValidatorBuilder jsonArrayNotEmpty() {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        final JsonArray array = (JsonArray) this.value;
        if (this.value != null && !array.iterator().hasNext()) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".cannot.be.empty");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                    .append("` cannot be empty. You must select at least one.");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter);
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public void expectedArrayButIsNot() {
        final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                .append(this.parameter).append(".is.not.an.array");
        final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                .append("` is not an array.");
        final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(), defaultEnglishMessage.toString(),
                this.parameter);
        this.dataValidationErrors.add(error);
    }

    public DataValidatorBuilder anyOfNotNull(final Object... object) {
        boolean hasData = false;
        for (final Object obj : object) {
            if (obj != null) {
                hasData = true;
                break;
            }
        }

        if (!hasData) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource)
                    .append(".no.parameters.for.update");
            final StringBuilder defaultEnglishMessage = new StringBuilder("No parameters passed for update.");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), "id");
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public DataValidatorBuilder inValidValue(final String parameterValueCode, final Object invalidValue) {
        final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                .append(this.parameter).append(".invalid.").append(parameterValueCode);
        final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                .append("` has an invalid value.");
        final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(), defaultEnglishMessage.toString(),
                this.parameter, invalidValue);
        this.dataValidationErrors.add(error);
        return this;
    }

    public DataValidatorBuilder mustBeBlankWhenParameterProvided(final String parameterName, final Object parameterValue) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value == null && parameterValue != null) {
            return this;
        }

        if (this.value != null && StringUtils.isBlank(this.value.toString()) && parameterValue != null
                && StringUtils.isNotBlank(parameterValue.toString())) {
            return this;
        }

        final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                .append(this.parameter).append(".cannot.also.be.provided.when.").append(parameterName).append(".is.populated");
        final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                .append("` cannot also be provided when `").append(parameterName).append("` is populated.");
        final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(), defaultEnglishMessage.toString(),
                this.parameter, this.value, parameterName, parameterValue);
        this.dataValidationErrors.add(error);
        return this;
    }

    public DataValidatorBuilder mustBeBlankWhenParameterProvidedIs(final String parameterName, final Object parameterValue) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value == null && parameterValue != null) {
            return this;
        }

        if (this.value != null && StringUtils.isBlank(this.value.toString()) && parameterValue != null
                && StringUtils.isNotBlank(parameterValue.toString())) {
            return this;
        }

        final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                .append(this.parameter).append(".cannot.also.be.provided.when.").append(parameterName).append(".is.")
                .append(parameterValue);
        final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                .append("` cannot also be provided when `").append(parameterName).append("` is ").append(parameterValue);
        final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(), defaultEnglishMessage.toString(),
                this.parameter, this.value, parameterName, parameterValue);
        this.dataValidationErrors.add(error);
        return this;
    }

    public DataValidatorBuilder cantBeBlankWhenParameterProvidedIs(final String parameterName, final Object parameterValue) {
        if (this.value != null && StringUtils.isNotBlank(this.value.toString())) {
            return this;
        }

        final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                .append(this.parameter).append(".must.be.provided.when.").append(parameterName).append(".is.").append(parameterValue);
        final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                .append("` must be provided when `").append(parameterName).append("` is ").append(parameterValue);
        final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(), defaultEnglishMessage.toString(),
                this.parameter, this.value, parameterName, parameterValue);
        this.dataValidationErrors.add(error);
        return this;
    }

    public DataValidatorBuilder compareMinimumAndMaximumAmounts(final BigDecimal minimumBalance, final BigDecimal maximumBalance) {
        if (minimumBalance != null && maximumBalance != null) {
            if (maximumBalance.compareTo(minimumBalance) < 0) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".is.not.within.expected.range");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` minimum amount ").append(minimumBalance).append(" should less than the maximum amount ")
                        .append(maximumBalance).append(".");
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, minimumBalance, maximumBalance);
                this.dataValidationErrors.add(error);
                return this;
            }
        }
        return this;
    }

    public DataValidatorBuilder inMinAndMaxAmountRange(final BigDecimal minimumAmount, final BigDecimal maximumAmount) {
        if (minimumAmount != null && maximumAmount != null && this.value != null) {
            final BigDecimal amount = BigDecimal.valueOf(Double.parseDouble(this.value.toString()));
            if (amount.compareTo(minimumAmount) < 0 || amount.compareTo(maximumAmount) > 0) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".amount.is.not.within.min.max.range");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter).append("` amount ")
                        .append(amount).append(" must be between ").append(minimumAmount).append(" and ").append(maximumAmount)
                        .append(" .");
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, amount, minimumAmount, maximumAmount);
                this.dataValidationErrors.add(error);
                return this;
            }
        }
        return this;
    }

    public DataValidatorBuilder notLessThanMin(final BigDecimal min) {
        if (min != null && this.value != null) {
            final BigDecimal amount = BigDecimal.valueOf(Double.parseDouble(this.value.toString()));
            if (amount.compareTo(min) < 0) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".is.less.than.min");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter).append("` value ")
                        .append(amount).append(" must not be less than the minimum value ").append(min);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, amount, min);
                this.dataValidationErrors.add(error);
                return this;
            }
        }
        return this;
    }

    public DataValidatorBuilder notGreaterThanMax(final BigDecimal max) {
        if (max != null && this.value != null) {
            final BigDecimal amount = BigDecimal.valueOf(Double.parseDouble(this.value.toString()));
            if (amount.compareTo(max) > 0) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".is.greater.than.max");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter).append("` value ")
                        .append(amount).append(" must not be more than maximum value ").append(max);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, amount, max);
                this.dataValidationErrors.add(error);
                return this;
            }
        }
        return this;
    }

    public DataValidatorBuilder compareMinAndMaxOfTwoBigDecmimalNos(final BigDecimal min, final BigDecimal max) {
        if (min != null && max != null) {
            if (max.compareTo(min) < 0) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".is.not.within.expected.range");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The ").append(" min number ").append(min)
                        .append(" should less than max number ").append(max).append(".");
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, min, max);
                this.dataValidationErrors.add(error);
                return this;
            }
        }
        return this;
    }

    public DataValidatorBuilder isValidRecurringRule(final String recurringRule) {
        if (StringUtils.isNotBlank(recurringRule)) {
            try {
                final RRule rRule = new RRule(recurringRule);
                rRule.validate();
            } catch (final ValidationException e) {
                final ApiParameterError error = ApiParameterError.parameterError("validation.msg.invalid.recurring.rule",
                        "The Recurring Rule value: " + recurringRule + " is not valid.", this.parameter, recurringRule);
                this.dataValidationErrors.add(error);
                return this;
            } catch (final ParseException e) {
                final ApiParameterError error = ApiParameterError.parameterError("validation.msg.recurring.rule.parsing.error",
                        "Error in parsing the Recurring Rule value: " + recurringRule + ".", this.parameter, recurringRule);
                this.dataValidationErrors.add(error);
                return this;
            }
        }
        return this;
    }

    public DataValidatorBuilder notLessThanMin(final Integer min) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null && min != null) {
            final Integer number = Integer.valueOf(this.value.toString());
            if (number < min) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".is.less.than.min");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be greater than the minimum value ").append(min);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, number, min);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder notGreaterThanMax(final Integer max) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null && max != null) {
            final Integer number = Integer.valueOf(this.value.toString());
            if (number > max) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".is.greater.than.max");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be less than the maximum value ").append(max);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, number, max);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder matchesRegularExpression(final String expression) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null && !this.value.toString().matches(expression)) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".does.not.match.regexp");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                    .append("` must match the provided regular expression [ ").append(expression).append(" ] ").append(".");

            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter, this.value, expression);

            this.dataValidationErrors.add(error);
        }

        return this;
    }

    public DataValidatorBuilder matchesRegularExpression(final String expression, final String Message) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null && !this.value.toString().matches(expression)) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".does.not.match.regexp");
            final StringBuilder defaultEnglishMessage = new StringBuilder(Message);

            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter, this.value, expression);

            this.dataValidationErrors.add(error);
        }

        return this;
    }

    private DataValidatorBuilder validateStringFor(final String validInputs) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }
        final Iterable<String> inputs = Splitter.onPattern(VALID_INPUT_SEPERATOR).split(validInputs);
        boolean validationErr = true;
        for (final String input : inputs) {
            if (input.equalsIgnoreCase(this.value.toString().trim())) {
                validationErr = false;
                break;
            }
        }
        if (validationErr) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".value.should.true.or.false");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                    .append("` value should true or false ");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter, this.value);
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public DataValidatorBuilder validateForBooleanValue() {
        return validateStringFor("TRUE" + VALID_INPUT_SEPERATOR + "FALSE");
    }

    public DataValidatorBuilder validatePhoneNumber() {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }
        boolean validationErr = true;
        /*
         * supports numbers, parentheses(), hyphens and may contain + sign in the beginning and can contain whitespaces
         * in between and length allowed is 0-25 chars.
         */
        final String regex = "^\\+?[0-9. ()-]{0,25}$";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(this.value.toString());
        if (matcher.matches()) {
            validationErr = false;
        }
        if (validationErr) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".format.is.invalid");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                    .append("` is in invalid format, should contain '-','+','()' and numbers only.");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter, this.value);
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public DataValidatorBuilder validateCronExpression() {
        if (this.value != null && !CronExpression.isValidExpression(this.value.toString().trim())) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".invalid");
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                    .append("` value is not a valid cron expression");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter, this.value);
            this.dataValidationErrors.add(error);
        }
        return this;
    }

    public DataValidatorBuilder validateDateAfter(final LocalDate date) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null && date != null) {
            final LocalDate dateVal = (LocalDate) this.value;
            if (date.isAfter(dateVal)) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".is.less.than.date");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be greater than the provided date").append(date);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, dateVal, date);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder validateDateBefore(final LocalDate date) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null && date != null) {
            final LocalDate dateVal = (LocalDate) this.value;
            if (date.isBefore(dateVal)) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".is.greater.than.date");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be less than the provided date").append(date);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, dateVal, date);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder validateDateBeforeOrEqual(final LocalDate date) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null && date != null) {
            final LocalDate dateVal = (LocalDate) this.value;
            if (dateVal.isAfter(date)) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".is.greater.than.date");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be less than or equal to the provided date: ").append(date);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, dateVal, date);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder validateDateForEqual(final LocalDate date) {
        if (this.value == null && this.ignoreNullValue) {
            return this;
        }

        if (this.value != null && date != null) {
            final LocalDate dateVal = (LocalDate) this.value;
            if (!dateVal.isEqual(date)) {
                final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                        .append(this.parameter).append(".is.not.equal.to.date");
                final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter)
                        .append("` must be equal to the provided date").append(date);
                final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                        defaultEnglishMessage.toString(), this.parameter, dateVal, date);
                this.dataValidationErrors.add(error);
            }
        }
        return this;
    }

    public DataValidatorBuilder scaleNotGreaterThan(Integer scale) {
        final BigDecimal value = BigDecimal.valueOf(Double.parseDouble(this.value.toString()));
        if (value.scale() > scale.intValue()) {
            final StringBuilder validationErrorCode = new StringBuilder("validation.msg.").append(this.resource).append(".")
                    .append(this.parameter).append(".scale.is.greater.than.").append(scale);
            final StringBuilder defaultEnglishMessage = new StringBuilder("The parameter `").append(this.parameter).append("` value ")
                    .append(value).append(" decimal place must not be more than ").append(scale).append(" places");
            final ApiParameterError error = ApiParameterError.parameterError(validationErrorCode.toString(),
                    defaultEnglishMessage.toString(), this.parameter, value, scale);
            this.dataValidationErrors.add(error);
            return this;
        }
        return this;
    }

    /**
     * Throws Exception if validation errors.
     *
     * @throws PlatformApiDataValidationException
     *             unchecked exception (RuntimeException) thrown if there are any validation error
     */
    public void throwValidationErrors() throws PlatformApiDataValidationException {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
