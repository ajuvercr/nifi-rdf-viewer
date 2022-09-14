/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.vlaanderen.informatievlaanderen.viewer;

import org.apache.nifi.web.ViewableContent;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.eclipse.rdf4j.rio.helpers.BasicWriterSettings.INLINE_BLANK_NODES;
import static org.eclipse.rdf4j.rio.helpers.BasicWriterSettings.PRETTY_PRINT;

public class RDFViewerController extends HttpServlet {

    private static final List<RDFFormat> formats = List.of(
            RDFFormat.TURTLE, RDFFormat.NTRIPLES, RDFFormat.N3, RDFFormat.JSONLD
    );

    private static final Set<String> supportedMimeTypes = new HashSet<>();

    static {
        for (FileFormat ff : formats) {
            supportedMimeTypes.addAll(ff.getMIMETypes());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final ViewableContent content = (ViewableContent) request.getAttribute(ViewableContent.CONTENT_REQUEST_ATTRIBUTE);

        String contentType = content.getContentType();

        if (supportedMimeTypes.contains(contentType)) {
            String formatted;

            if (content.getDisplayMode().equals(ViewableContent.DisplayMode.Formatted)) {
                RDFFormat format = RDFFormat.matchMIMEType(contentType, formats).get();
                Model model = Rio.parse(content.getContentStream(), format);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                RDFWriter streamWriter = Rio.createWriter(format, stream);

                WriterConfig config = new WriterConfig();
                config.set(PRETTY_PRINT, true);
                config.set(INLINE_BLANK_NODES, true);
                streamWriter.setWriterConfig(config);

                Rio.write(model, streamWriter);
                // defer to the jsp
                formatted = stream.toString();
            } else {
                formatted = content.getContent();
            }

            request.setAttribute("mode", contentType);
            request.setAttribute("content", formatted);
            request.getRequestDispatcher("/WEB-INF/jsp/codemirror.jsp").include(request, response);
        } else {
            final PrintWriter out = response.getWriter();
            out.println("Unexpected content type: " + contentType);
        }
    }
}
