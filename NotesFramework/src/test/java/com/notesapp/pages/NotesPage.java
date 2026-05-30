package com.notesapp.pages;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class NotesPage {

    WebDriver driver;
    WebDriverWait wait;

    By addNoteBtn  = By.xpath("//*[@id='root']/div/div/div[2]/div/div[2]/div[2]/button");
    By modal       = By.xpath("//div[@role='dialog' and contains(@class,'show')]");
    By titleInput  = By.xpath("//div[@role='dialog']//input[@id='title']");
    By descInput   = By.xpath("//div[@role='dialog']//textarea[@id='description']");
    By categoryDdl = By.xpath("//div[@role='dialog']//select[@id='category']");
    By saveBtn     = By.xpath(
        "//div[@role='dialog']//button[@type='submit' and not(@id='search-btn')]");

    public NotesPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void clickAddNote() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(addNoteBtn));
        WebElement btn = driver.findElement(addNoteBtn);
        System.out.println("Add Note button text: " + btn.getText());

        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView(true);", btn);
        try {
            btn.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
            System.out.println("Used JS click for Add Note button");
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(modal));
        wait.until(ExpectedConditions.visibilityOfElementLocated(titleInput));
        System.out.println("Note creation modal opened");
    }

    public void fillNoteForm(String title, String description, String category) {
        driver.findElement(titleInput).clear();
        driver.findElement(titleInput).sendKeys(title);
        driver.findElement(descInput).clear();
        driver.findElement(descInput).sendKeys(description);
        new Select(driver.findElement(categoryDdl)).selectByVisibleText(category);
    }

    public void submitNote(String expectedTitle) {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(saveBtn));
        System.out.println("Save button found: " + btn.getText());
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);

        // don't wait for modal to disappear — it may linger due to Chrome dialogs
        // instead wait for the note title to appear in the list which confirms the save worked
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'" + expectedTitle + "')]")));
            System.out.println("Note saved and visible: " + expectedTitle);
        } catch (Exception e) {
            // if the title isn't visible yet, just log and continue
            // isNoteVisible() in the test will do the real assertion
            System.out.println("Note save submitted (visibility check pending): " + expectedTitle);
        }
    }

    public void createNote(String title, String description, String category) {
        clickAddNote();
        fillNoteForm(title, description, category);
        submitNote(title);
        System.out.println("Note submitted: " + title);
    }

    public boolean isNoteVisible(String title) {
        try {
            By locator = By.xpath("//*[contains(text(),'" + title + "')]");
            wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            System.out.println("Note not found in UI: " + title);
            return false;
        }
    }

    public boolean isNoteGone(String title) {
        try {
            By locator = By.xpath("//*[contains(text(),'" + title + "')]");
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.invisibilityOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}