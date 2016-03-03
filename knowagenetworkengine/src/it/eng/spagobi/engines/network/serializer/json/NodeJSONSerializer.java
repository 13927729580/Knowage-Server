/*
 * Knowage, Open Source Business Intelligence suite
 * Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.
 * 
 * Knowage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Knowage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.eng.spagobi.engines.network.serializer.json;

import it.eng.spagobi.engines.network.bean.Node;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @authors Alberto Ghedin (alberto.ghedin@eng.it)
 *
 */
public class NodeJSONSerializer extends JsonSerializer<Node> {

	@Override
	public void serialize(Node value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException{
		String key;
		jgen.writeStartObject();
		jgen.writeStringField("id", value.getId());
		Iterator<String> iterator = value.getProperties().keySet().iterator();
		while(iterator.hasNext()){
			key = iterator.next();
			jgen.writeStringField(key,value.getProperties().get(key));
		}
		jgen.writeEndObject();
	}
}
