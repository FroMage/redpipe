package net.redpipe.example.wiki.keycloakJooq;

import java.util.Arrays;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.vertx.core.json.JsonObject;

public class ApiUpdateValidator implements ConstraintValidator<ApiUpdateValid, JsonObject> {

	private String[] checkKeys;

	@Override
	public void initialize(ApiUpdateValid checkAnnot) {
		this.checkKeys = checkAnnot.value();
	}

	@Override
	public boolean isValid(JsonObject page, ConstraintValidatorContext context) {
		if (!Arrays.stream(checkKeys).allMatch(page::containsKey)) {
			System.err.println("Bad page creation JSON payload: " + page.encodePrettily());
			return false;
		}
		return true;
	}

}
