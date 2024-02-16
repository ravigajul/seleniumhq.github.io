package dev.selenium.bidirectional.webdriver_bidi;

import dev.selenium.BaseTest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.bidi.Script;
import org.openqa.selenium.bidi.browsingcontext.BrowsingContext;
import org.openqa.selenium.bidi.browsingcontext.ReadinessState;
import org.openqa.selenium.bidi.script.EvaluateResult;
import org.openqa.selenium.bidi.script.EvaluateResultExceptionValue;
import org.openqa.selenium.bidi.script.EvaluateResultSuccess;
import org.openqa.selenium.bidi.script.LocalValue;
import org.openqa.selenium.bidi.script.RealmInfo;
import org.openqa.selenium.bidi.script.RemoteReference;
import org.openqa.selenium.bidi.script.ResultOwnership;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

class ScriptTest extends BaseTest {

    @BeforeEach
    public void setup() {
        FirefoxOptions options = new FirefoxOptions();
        options.setCapability("webSocketUrl", true);
        driver = new FirefoxDriver(options);
    }

    @Test
    void canCallFunctionWithDeclaration() {
        String id = driver.getWindowHandle();
        try (Script script = new Script(id, driver)) {
            EvaluateResult result =
                    script.callFunctionInBrowsingContext(
                            id, "()=>{return 1+2;}", false, Optional.empty(), Optional.empty(), Optional.empty());
            EvaluateResultSuccess successResult = (EvaluateResultSuccess) result;
            Assertions.assertEquals(3L, (Long) successResult.getResult().getValue().get());
        }
    }

    @Test
    void canCallFunctionWithAwaitPromise() {
        String id = driver.getWindowHandle();
    try (Script script = new Script(id, driver)) {
        EvaluateResult result =
                script.callFunctionInBrowsingContext(
                        id,
                        "async function() {{\n"
                                + "            await new Promise(r => setTimeout(() => r(), 0));\n"
                                + "            return \"SOME_DELAYED_RESULT\";\n"
                                + "          }}",
                        true,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty());
        EvaluateResultSuccess successResult = (EvaluateResultSuccess) result;
        Assertions.assertEquals("SOME_DELAYED_RESULT", (String) successResult.getResult().getValue().get());
        }
    }

    @Test
    void canCallFunctionWithOwnershipRoot() {
        String id = driver.getWindowHandle();
        try (Script script = new Script(id, driver)) {
            EvaluateResult result =
                    script.callFunctionInBrowsingContext(
                            id,
                            "async function(){return {a:1}}",
                            true,
                            Optional.empty(),
                            Optional.empty(),
                            Optional.of(ResultOwnership.ROOT));

            EvaluateResultSuccess successResult = (EvaluateResultSuccess) result;
            Assertions.assertTrue(successResult.getResult().getHandle().isPresent());
            }
        }

    @Test
    void canCallFunctionThatThrowsException() {
        String id = driver.getWindowHandle();
    try (Script script = new Script(id, driver)) {
        EvaluateResult result =
                script.callFunctionInBrowsingContext(
                        id,
                        "))) !!@@## some invalid JS script (((",
                        false,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty());
        EvaluateResultExceptionValue exception = (EvaluateResultExceptionValue) result;
        Assertions.assertEquals("error", exception.getExceptionDetails().getException().getType());
        Assertions.assertEquals(
                "SyntaxError: expected expression, got ')'", exception.getExceptionDetails().getText());
        }
    }

    @Test
    void canCallFunctionInASandBox() {
        String id = driver.getWindowHandle();
        try (Script script = new Script(id, driver)) {
            EvaluateResult result =
                    script.callFunctionInBrowsingContext(
                            id,
                            "sandbox",
                            "() => window.foo",
                            true,
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty());

        Assertions.assertEquals(EvaluateResult.Type.SUCCESS, result.getResultType());
        }
    }

    @Test
    void canCallFunctionInARealm() {
        String tab = driver.getWindowHandle();
        try (Script script = new Script(tab, driver)) {
            List<RealmInfo> realms = script.getAllRealms();
            String realmId = realms.get(0).getRealmId();

            EvaluateResult result = script.callFunctionInRealm(
                    realmId,
                    "() => { window.foo = 3; }",
                    true,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());

            Assertions.assertEquals(EvaluateResult.Type.SUCCESS, result.getResultType());
        }
    }

    @Test
    void canEvaluateScript() {
        String id = driver.getWindowHandle();

        try (Script script = new Script(id, driver)) {
            EvaluateResult result =
                    script.evaluateFunctionInBrowsingContext(id, "1 + 2", true, Optional.empty());

            EvaluateResultSuccess successResult = (EvaluateResultSuccess) result;
            Assertions.assertEquals(EvaluateResult.Type.SUCCESS, result.getResultType());
            Assertions.assertEquals(3L, (Long) successResult.getResult().getValue().get());
        }
    }

    @Test
    void canEvaluateScriptThatThrowsException() {
        String id = driver.getWindowHandle();
        try (Script script = new Script(id, driver)) {
            EvaluateResult result =
                    script.evaluateFunctionInBrowsingContext(
                            id, "))) !!@@## some invalid JS script (((", false, Optional.empty());

            EvaluateResultExceptionValue exception = (EvaluateResultExceptionValue) result;
            Assertions.assertEquals("error", exception.getExceptionDetails().getException().getType());
            Assertions.assertEquals(
                    "SyntaxError: expected expression, got ')'", exception.getExceptionDetails().getText());
        }
    }

    @Test
    void canEvaluateScriptWithResulWithOwnership() {
        String id = driver.getWindowHandle();
        try (Script script = new Script(id, driver)) {
            EvaluateResult result =
                    script.evaluateFunctionInBrowsingContext(
                            id, "Promise.resolve({a:1})", true, Optional.of(ResultOwnership.ROOT));

            EvaluateResultSuccess successResult = (EvaluateResultSuccess) result;
            Assertions.assertTrue(successResult.getResult().getHandle().isPresent());
        }
    }

    @Test
    void canEvaluateInASandBox() {
        String id = driver.getWindowHandle();
        try (Script script = new Script(id, driver)) {
            EvaluateResult result =
                    script.evaluateFunctionInBrowsingContext(
                            id, "sandbox", "window.foo", true, Optional.empty());


            Assertions.assertEquals(EvaluateResult.Type.SUCCESS, result.getResultType());
        }
    }

    @Test
    void  canEvaluateInARealm() {
        String tab = driver.getWindowHandle();
        try (Script script = new Script(tab, driver)) {
            List<RealmInfo> realms = script.getAllRealms();
            String realmId = realms.get(0).getRealmId();

            EvaluateResult result =
                    script.evaluateFunctionInRealm(
                            realmId, "window.foo", true, Optional.empty());

            Assertions.assertEquals(EvaluateResult.Type.SUCCESS, result.getResultType());
        }
    }

    @Test
    void  canDisownHandle() {
        String window = driver.getWindowHandle();
        try (Script script = new Script(window, driver)) {
            BrowsingContext context = new BrowsingContext(driver, window);

            context.navigate("https://www.selenium.dev/selenium/web/dynamic.html", ReadinessState.COMPLETE);

            driver.findElement(By.id("adder")).click();

            getLocatedElement(driver, By.id("box0"));

            String boxId = (String) ((JavascriptExecutor)driver).executeScript(
                    "var box = document.querySelector('.redbox');" +
                            "return box.id;");

            script.disownBrowsingContextScript(
                            window, List.of(boxId));

            LocalValue value =
                    LocalValue.remoteReference(
                            RemoteReference.Type.HANDLE, boxId);

            // Since the handle is now eligible for garbage collections, it is no longer available to be used.
            Assertions.assertThrows(WebDriverException.class, () ->script.callFunctionInBrowsingContext(
                    window,
                    "arg => arg.a",
                    false, Optional.of(List.of(value)),
                    Optional.empty(),
                    Optional.empty()));
        }
    }
}
