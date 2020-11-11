/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.bdd.steps.ui.web;

import static org.hamcrest.Matchers.containsString;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.openqa.selenium.WebElement;
import org.vividus.bdd.steps.ComparisonRule;
import org.vividus.bdd.steps.ui.validation.IBaseValidations;
import org.vividus.bdd.steps.ui.web.model.JsArgument;
import org.vividus.bdd.steps.ui.web.model.JsArgumentType;
import org.vividus.softassert.ISoftAssert;
import org.vividus.ui.action.search.Locator;
import org.vividus.ui.action.search.SearchParameters;
import org.vividus.ui.action.search.Visibility;
import org.vividus.ui.web.action.WebJavascriptActions;
import org.vividus.ui.web.action.search.WebLocatorType;
import org.vividus.ui.web.util.LocatorUtil;

public class CodeSteps
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject private IBaseValidations baseValidations;
    @Inject private WebJavascriptActions javascriptActions;
    @Inject private ISoftAssert softAssert;

    /**
     * Executes JavaScript code with specified arguments
     * @param jsCode JavaScript code
     * @param args A table containing JS command argument value and type (one of <i>STRING</i> or <i>OBJECT</i>)<br>
     * Example:
     * <p>
     * <code>
     * When I execute javaScript `remote:throttle` with arguments:<br>
     * |value                |type  |<br>
     * |{"condition": "Wifi"}|object|
     * </code>
     * </p>
     */
    @When("I execute javascript `$jsCode` with arguments:$args")
    public void executeJavascriptWithArguments(String jsCode, List<JsArgument> args)
    {
        javascriptActions.executeScript(jsCode, args.stream().map(this::convertRowToArgument).toArray());
    }

    private Object convertRowToArgument(JsArgument arg)
    {
        JsArgumentType type = arg.getType();
        String value = arg.getValue();
        if (type == null || value == null)
        {
            throw new IllegalArgumentException("Please, specify command argument values and types");
        }
        return type.convert(value);
    }

    /**
     * Checks that the <b>JSON</b> message returned after the <i>javascript call</i> contains the certain
     * <b>field</b> and checks the <b>value</b> of this field.
     * <p>
     * To make a JavaScript call one can open a <b><i>JavaScript console</i></b> in a browser <i>(via Ctrl+Shift+J)</i>,
     * type the <b>script</b> string and press <i>Enter</i>.
     * <p>
     * Actions performed at this step:
     * <ul>
     * <li>Removes the value of JSON message field;
     * <li>Checks the value with the specified one.
     * </ul>
     * @param script A string with the JavaScript command which returns a JSON message as a result
     * @param fieldName The name of the required field in a JSON message
     * @param value The required value of the field in a JSON message
     * @throws IOException If an input or output exception occurred
     * @see <a href="https://www.w3schools.com/json/"> <i>JSON format</i></a>
     */
    @Then("json generated by javascript `$script` contains field with name `$fieldName' and value `$value`")
    public void checkJsonFieldValue(String script, String fieldName, String value) throws IOException
    {
        JsonNode json = MAPPER.readTree(javascriptActions.executeScript("return JSON.stringify(" + script + ")")
                .toString());
        JsonNode actualFieldValue = json.get(fieldName);
        if (softAssert.assertNotNull(
                String.format("Field with the name '%s' was found in the JSON object", fieldName), actualFieldValue))
        {
            softAssert.assertEquals(
                    String.format("Field value from the JSON object is equal to '%s'", value),
                    value, actualFieldValue.textValue());
        }
    }

    /**
     * Steps checks the <a href="https://en.wikipedia.org/wiki/Favicon">favicon</a> of the tab
     * of the browser. It is displayed in DOM in the &lt;head&gt; tag.
     * <b> Example</b>
     * <pre>
     * &lt;head profile="http://www.w3.org/1999/xhtml/vocab"&gt;
     * &lt;link type="image/png" href="http://promo1dev/sites/promo1/files/<b>srcpart</b>" rel="shortcut icon" /&gt;
     * &lt;/head&gt;
     * </pre>
     * @param srcpart Part of URL with favicon
     */
    @Then("favicon with src containing `$srcpart` exists")
    public void ifFaviconWithSrcExists(String srcpart)
    {
        WebElement faviconElement = baseValidations.assertIfElementExists("Favicon",
                new Locator(WebLocatorType.XPATH,
                        new SearchParameters(LocatorUtil.getXPath("//head/link[@rel='shortcut icon' or @rel='icon']"),
                                Visibility.ALL)));
        if (faviconElement != null)
        {
            String href = faviconElement.getAttribute("href");
            if (softAssert.assertNotNull("Favicon contains 'href' attribute", href))
            {
                softAssert.assertThat("The favicon with the src containing " + srcpart + " exists", href,
                        containsString(srcpart));
            }
        }
    }

    /**
     * Checks that the <b>page code</b> contains a <b>node element</b> specified by <b>Locator</b>
     * @param locator A locator for an expected element
     * @param quantity the number to compare
     * @param comparisonRule The rule to compare values
     * (<i>Possible values: <b>LESS_THAN or &lt;, LESS_THAN_OR_EQUAL_TO or &lt;=, GREATER_THAN or &gt;,
     * GREATER_THAN_OR_EQUAL_TO or &gt;=, EQUAL_TO or =, NOT_EQUAL_TO or !=</b></i>)
     * @return <b>List&lt;WebElement&gt;</b> A collection of elements matching the locator,
     * <b> null</b> - if there are no desired elements
     */
    @Then("number of invisible elements `$locator` is $comparisonRule `$quantity`")
    public List<WebElement> doesInvisibleQuantityOfElementsExists(Locator locator,
            ComparisonRule comparisonRule, int quantity)
    {
        locator.getSearchParameters().setVisibility(Visibility.ALL);
        return baseValidations.assertIfNumberOfElementsFound("The number of found invisible elements",
                    locator, quantity, comparisonRule);
    }

    public void setJavascriptActions(WebJavascriptActions javascriptActions)
    {
        this.javascriptActions = javascriptActions;
    }

    public void setBaseValidations(IBaseValidations baseValidations)
    {
        this.baseValidations = baseValidations;
    }

    public void setSoftAssert(ISoftAssert softAssert)
    {
        this.softAssert = softAssert;
    }
}
