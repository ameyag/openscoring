/*
 * Copyright (c) 2013 Villu Ruusmann
 *
 * This file is part of Openscoring
 *
 * Openscoring is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Openscoring is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Openscoring.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openscoring.server;

import java.io.*;
import java.util.*;

import org.openscoring.common.*;

import com.google.common.collect.*;

import org.supercsv.io.*;
import org.supercsv.prefs.*;

public class CsvUtil {

	private CsvUtil(){
	}
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	static
	public CsvPreference getFormat(BufferedReader reader) throws IOException {
		reader.mark(10 * 1024);

		CsvPreference[] formats = {CsvPreference.EXCEL_PREFERENCE, CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE, CsvPreference.TAB_PREFERENCE};
		for(CsvPreference format : formats){

			try {
				if(checkFormat(reader, format)){
					return format;
				}
			} finally {
				reader.reset();
			}
		}

		throw new IOException("Unrecognized CSV format");
	}

	static
	private boolean checkFormat(BufferedReader reader, CsvPreference format) throws IOException {
		CsvListReader parser = new CsvListReader(reader, format);

		int columns = 0;

		// Check the header line and the first ten lines
		for(int line = 0; line < (1 + 10); line++){
			List<String> row = parser.read();
			if(row == null){
				break;
			} // End if

			if(columns == 0 || columns == row.size()){
				columns = row.size();
			} else

			{
				columns = -1;

				break;
			}
		}

		return (columns > 2);
	}

	static
	public List<EvaluationRequest> readTable(BufferedReader reader, CsvPreference format) throws IOException {
		List<EvaluationRequest> requests = Lists.newArrayList();

		CsvMapReader parser = new CsvMapReader(reader, format);

		String[] header = parser.getHeader(true);

		while(true){
			Map<String, String> arguments = parser.read(header);
			if(arguments == null){
				break;
			}

			EvaluationRequest request = new EvaluationRequest();
			request.setArguments(arguments);

			requests.add(request);
		}

		parser.close();

		return requests;
	}

	static
	public void writeTable(BufferedWriter writer, CsvPreference format, List<EvaluationResponse> responses) throws IOException {
		CsvMapWriter formatter = new CsvMapWriter(writer, format);

		String[] header = null;
        lock.writeLock.lock();
        
        try
        {

            for(EvaluationResponse response : responses){
                Map<String, ?> result = response.getResult();

                if(header == null){
                    Set<String> keys = result.keySet();

                    header = (keys).toArray(new String[keys.size()]);

                    formatter.writeHeader(header);
                }

                formatter.write(result, header);
            }
        
		formatter.flush();
		formatter.close();
        }
        finally {
            lock.writeLock.unlock();
        }
	}
}