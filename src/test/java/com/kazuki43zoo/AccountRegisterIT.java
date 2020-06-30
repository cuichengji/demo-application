package com.kazuki43zoo;

import com.kazuki43zoo.pages.WelcomePage;
import com.kazuki43zoo.pages.password.PasswordChangePage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:META-INF/spring/seleniumContext.xml"})
public class AccountRegisterIT {

	private static WebDriver driver;

	@Value("${selenium.applicationContextUrl}")
	private String applicationContextUrl;

	@BeforeClass
	public static void setUp(){
		WebDriverManager.chromedriver().setup();
		driver = new ChromeDriver(new ChromeOptions().addArguments("--headless"));
	}

	@Test
	public void registerNewAccount() {

		driver.get(applicationContextUrl);

		String accountId = "kazuki43zoo";
		String password = "password";

		PasswordChangePage passwordChangePage = new WelcomePage(driver)
				.username("user01").password("password").login()
				.getLeftMenu().accountManagement()
				.create()
					.accountId(accountId)
					.firstName("Kazuki")
					.lastName("Shimizu")
					.enabled(true)
					.enabledAutoLogin(false)
					.password(password)
					.confirmPassword(password)
					.authorities("ADMIN", "USER", "ACCOUNTMNG")
					.save()
				.getUserMenuPullDown().logout()
				.username(accountId).password(password).login(PasswordChangePage.class);

		assertThat(passwordChangePage.getAlert().getText(),
				is("Password has not initialized. Please initialize the password."));

	}

	@AfterClass
	public static void tearDown() {
		driver.quit();
	}

}

