package com.example.config;

import com.example.util.TimezoneContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class InstantSerializer extends JsonSerializer<Instant> {

	@Override
	public void serialize(Instant value,
	                      JsonGenerator gen,
	                      SerializerProvider serializers) throws IOException {

	    ZoneId zoneId = TimezoneContext.getZone();
	    ZonedDateTime zonedDateTime = value.atZone(zoneId);

	    DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	    gen.writeString(zonedDateTime.format(formatter));
	}
    
}