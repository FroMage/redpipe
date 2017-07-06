package org.mygroup.vertxrs;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class MyExceptionMapper implements ExceptionMapper<MyException>
{
   public Response toResponse(MyException exception) {
      return Response.status(500).entity("Yup, we got this").build();
   }
}