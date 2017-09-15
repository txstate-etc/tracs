/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.txstate.tr.util;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.log4j.Logger;
import java.util.List;

import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.cluster.api.ClusterService;


/**
 * $Author: wb15 $
 * $Date: 2010-02-26 09:45:19 -0600 (Fri, 26 Feb 2010) $
 * $Rev: 30 $
 * $Header: https://svn.tr.txstate.edu/svn/tracs/trunk/appserverpicker/src/main/java/edu/txstate/tr/util/appserverpicker.java 30 2010-02-26 15:45:19Z wb15 $
 * $Id: appserverpicker.java 30 2010-02-26 15:45:19Z wb15 $
 * @author Will Bending <will.bending@txstate.edu>
 * Java Servlet port of Jeff Snider's appserverpicker.pl
 */

public class appserverpicker extends HttpServlet {

   private static Logger log = Logger.getLogger(appserverpicker.class.getClass());
   private static ClusterService clusterService = (ClusterService) ComponentManager.get(ClusterService.class);

   /**
    * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
    * @param request servlet request
    * @param response servlet response
    * @throws ServletException if a servlet-specific error occurs
    * @throws IOException if an I/O error occurs
    */
   protected void processRequest(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException {
      response.setContentType("text/html;charset=UTF-8");
      PrintWriter out = response.getWriter();

      try {
         String cookie_value = "foobar";
         Cookie[] cookies = request.getCookies();
         if ( cookies != null ) {
            for (int i = 0; i < cookies.length; i++) {
               if ( cookies[i].getName().equals("JSESSIONID") ) {
                  cookie_value = cookies[i].getValue();
               }
            }
         }

         String current_server = "";
         if ( !cookie_value.equals("foobar") ) {
            Matcher matcher = Pattern.compile("^[a-f0-9-]*\\.([\\w-]+)$").matcher(cookie_value);
            if ( matcher.matches() ) {
              current_server = matcher.group(1);
            }
         }

         /* Runs only when they posted a ?setappserver=blah */
         String appServerToSet = request.getParameter("setappserver");
         if (appServerToSet != null) {
            Cookie newCookie = new Cookie("JSESSIONID",
                  "foobar." + appServerToSet);
            current_server = appServerToSet;
            response.addCookie(newCookie);
         }

         out.println("<html>\n" +
            "<head>\n" +
            "</head>\n" +
            "<body>\n" +
            "<ul>");

         List<String> instances = clusterService.getServers();
         for ( String instance : instances ) {
            Matcher matcher = Pattern.compile("^(.*)-\\d+$").matcher(instance);
            if ( matcher.matches() ) {
              out.print("<li><a href=\"?setappserver=" + matcher.group(1) + "\">" + matcher.group(1) + "</a>");
              if (current_server.equals(matcher.group(1))) {
                 out.print(" <b>(Selected)</b>");
              }
            }
            out.println("</li>");
         }

         out.println("</ul>\n<a href=\"/portal/login\">Login to TRACS</a>\n</body>\n</html>");

      } catch (Exception e) {
         /* Do something with fatal exceptions */
         out.println("Sorry, this page encountered an error.  Please try again.");
         log.fatal("A fatal error has occurred. Dumping StackTrace()..");
         e.printStackTrace();
         
      } finally {
         out.flush();
         out.close();
      }
   }

   // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
   /**
    * Handles the HTTP <code>GET</code> method.
    * @param request servlet request
    * @param response servlet response
    * @throws ServletException if a servlet-specific error occurs
    * @throws IOException if an I/O error occurs
    */
   @Override
   protected void doGet(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException {
      processRequest(request, response);
   }

   /**
    * Handles the HTTP <code>POST</code> method.
    * @param request servlet request
    * @param response servlet response
    * @throws ServletException if a servlet-specific error occurs
    * @throws IOException if an I/O error occurs
    */
   @Override
   protected void doPost(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException {
      processRequest(request, response);
   }

   /**
    * Returns a short description of the servlet.
    * @return a String containing servlet description
    */
   @Override
   public String getServletInfo() {
      return "appserverpicker";
   }// </editor-fold>
}
