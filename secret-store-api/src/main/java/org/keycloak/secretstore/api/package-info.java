@XmlJavaTypeAdapters({@XmlJavaTypeAdapter(type=ZonedDateTime.class,value=ZonedDateTimeXmlAdapter.class)})
package org.keycloak.secretstore.api;

import org.keycloak.secretstore.api.internal.ZonedDateTimeXmlAdapter;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import java.time.ZonedDateTime;

