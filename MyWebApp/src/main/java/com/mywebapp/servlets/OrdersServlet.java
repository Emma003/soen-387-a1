package com.mywebapp.servlets;

import com.mywebapp.ConfigManager;
import com.mywebapp.logic.LogicFacade;
import com.mywebapp.logic.custom_errors.*;
import com.mywebapp.logic.models.Order;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

@WebServlet(name = "ordersServlet", value = {"/orders/*", "/orderForm", "/createOrder", "/shipOrder"})
public class OrdersServlet {
    LogicFacade logic = new LogicFacade();
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException{
        String url = request.getRequestURI();

        if (url.equals("/orderForm")){
            response.setStatus(HttpServletResponse.SC_OK);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/ordersForm.jsp");
            dispatcher.forward(request, response);
        }
        else if (url.equals("/orders")){
            String password = request.getParameter("password");
            String type = "user";
            boolean found = false;
            File users_file = null;
            String customerId = null;
            ArrayList<Order> orders = null;

            try {
                users_file = new File(ConfigManager.getCsvPath());

                try (CSVReader reader = new CSVReader(new FileReader(users_file))) {
                    String[] line;
                    while ((line = reader.readNext()) != null) {
                        String newPass = line[0];
                        type = line[1];
                        if (newPass.equals("password")) { // skip if the first row (titles) is being read
                            continue;
                        }
                        if (password.equals(newPass)) {
                            found = true;
                            break;
                        }
                    }
                }

                customerId = getCustomerID(password);

                if (found) {
                    if (type.equals("user")) {
                        orders = logic.getOrdersByCustomer(customerId);
                    } else {
                        orders = logic.getAllOrders();
                    }
                }
            } catch (FileDownloadException | CsvValidationException | UserNotFoundException | DataMapperException | OrderNotFoundException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            response.setStatus(HttpServletResponse.SC_OK);
            request.setAttribute("orders", orders);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/ordersDisplay.jsp");
            dispatcher.forward(request, response);

        }

        else if(url.startsWith("/orders/:")) {
            String[] fullUrl = url.split("/");
            String urlSlug = fullUrl[fullUrl.length-1];
            String[] fullOrderId = urlSlug.split(":");
            int orderId = Integer.parseInt(fullOrderId[fullOrderId.length-1]);
            String password = request.getParameter("password");
            String customerId = null;
            Order order = null;
            try {
                customerId = getCustomerID(password);
                order = logic.getOrderDetails(customerId, orderId);
            } catch (CsvValidationException | FileDownloadException | DataMapperException | CustomerOrderMismatchException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (UserNotFoundException | OrderNotFoundException e) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            response.setStatus(HttpServletResponse.SC_OK);
            request.setAttribute("order", order);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/orderListing.jsp");
            dispatcher.forward(request,response);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException{
        String url = request.getRequestURI();

        if (url.equals("/createOrder")){
            String password = request.getParameter("password");
            String customerId = null;
            String shippingAddress = request.getParameter("shippingAddress");
            try {
                customerId = getCustomerID(password);
                logic.createOrder(customerId, shippingAddress);
            } catch (CsvValidationException | FileDownloadException | DataMapperException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (UserNotFoundException e) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            response.setStatus(HttpServletResponse.SC_OK);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/ordersDisplay.jsp");
            dispatcher.forward(request, response);
        }
        if (url.equals("/shipOrder")){
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            try {
                logic.shipOrder(orderId);
            } catch (DataMapperException | OrderNotFoundException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            response.setStatus(HttpServletResponse.SC_OK);
            RequestDispatcher dispatcher = request.getRequestDispatcher("displayOrders.jsp");
            dispatcher.forward(request, response);

        }
    }
    private String getCustomerID(String password) throws CsvValidationException, IOException, FileDownloadException {
        String customerId = "";
        try (CSVReader reader = new CSVReader(new FileReader(ConfigManager.getCsvPath()))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                String newPass = line[0];
                if (newPass.equals("password")) { // skip if the first row (titles) is being read
                    continue;
                }
                if(password.equals(newPass)){
                    customerId = line[0];
                    break;
                }
            }
        }
        return customerId;
    }
}
