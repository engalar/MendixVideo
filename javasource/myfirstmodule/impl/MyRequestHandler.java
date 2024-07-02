package myfirstmodule.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.mendix.core.Core;
import com.mendix.externalinterface.connector.RequestHandler;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import com.mendix.systemwideinterfaces.core.IContext;

import system.proxies.FileDocument;

public class MyRequestHandler extends RequestHandler {

    @Override
    protected void processRequest(IMxRuntimeRequest request, IMxRuntimeResponse response, String path)
            throws Exception {
        var context = Core.createSystemContext();
        var fileid = 0;
        if (request.getParameter("fileid") != null && !request.getParameter("fileid").isEmpty()) {
            fileid = Integer.parseInt(request.getParameter("fileid"));
        }
        if (fileid > 0) {
            var fileDocuments = Core
                    .createXPathQuery(
                            "//" + FileDocument.entityName + "[" + FileDocument.MemberNames.FileID + "=" + fileid + "]")
                    .execute(context);
            // must get one fileDocument
            if (fileDocuments.size() == 1) {
                var fileDocument = fileDocuments.get(0);
                processFileDocument(request, response, FileDocument.initialize(context, fileDocument), context);
            } else {
                response.setStatus(404);
            }
        } else {

            var guid = request.getParameter("guid");
            var fileDocument = FileDocument.load(context, Core.createMendixIdentifier(guid));
            processFileDocument(request, response, fileDocument, context);
        }
    }

    private void processFileDocument(IMxRuntimeRequest request, IMxRuntimeResponse response,
            FileDocument fileDocument, IContext context) throws IOException {

        var fileLength = fileDocument.getSize();
        var inputStream = Core.getFileDocumentContent(context, fileDocument.getMendixObject());

        var rangeHeader = request.getHeader("Range");
        if (rangeHeader != null) {
            var range = rangeHeader.replace("bytes=", "").split("-");
            var start = Long.parseLong(range[0]);
            var end = range.length > 1 && !range[1].isEmpty() ? Long.parseLong(range[1]) : fileLength - 1;
            int size = Math.min((int) (end - start + 1), 5 * 1024 * 1024);

            response.setStatus(206);
            response.addHeader("Accept-Ranges", "bytes");
            response.addHeader("Content-Type", "video/mp4");
            response.addHeader("Transfer-Encoding", "chunked");

            response.addHeader("Content-Disposition", "inline; filename=\"abc.mp4" + "\"");
            response.addHeader("Vary", "Accept-Encoding");
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Content-Security-Policy",
                    "default-src 'self' 'unsafe-eval' 'unsafe-inline'; script-src 'self' https://unpkg.com;  connect-src 'self' https://unpkg.com;");
            response.addHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.addHeader("Server", "MinIO Console");
            response.addHeader("X-Content-Type-Options", "nosniff");
            response.addHeader("X-Frame-Options", "SAMEORIGIN");
            response.addHeader("X-Xss-Protection", "1; mode=block");
            response.addHeader("Connection", "close");
            response.addHeader("Content-Range",
                    "bytes " + start + "-" + (start + size - 1) + "/" + fileLength);

            this.transferRange(response.getOutputStream(), inputStream, start, size);
        } else {
            response.setStatus(200);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, bytesRead);
            }
        }
        inputStream.close();
    }

    /**
     * Transfer a range of bytes from the input stream to the output stream.
     * 
     * @param outputStream the output stream to write the range to
     * @param inputStream  the input stream to read the range from
     * @param start        the start position of the range to transfer
     * @param size         the maximum size of the range to transfer
     * @return actual size of the transferred range
     * @throws IOException
     */
    private Integer transferRange(OutputStream outputStream, InputStream inputStream, long start, int size)
            throws IOException {
        long skipped = inputStream.skip(start);
        if (skipped < start) {
            throw new IOException("Failed to skip to the start position");
        }

        byte[] buffer = new byte[4 * 1024];
        int bytesRead = 0;
        int totalBytesTransferred = 0;
        // var a = inputStream.available();
        // System.out.println("available " + a);

        while (totalBytesTransferred < size && (bytesRead = inputStream.read(buffer, 0,
                Math.min(buffer.length, size - totalBytesTransferred))) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            // System.out.println(String.format("read %d bytes : %d", bytesRead,
            // totalBytesTransferred));
            totalBytesTransferred += bytesRead;
        }

        return totalBytesTransferred;

    }

}
