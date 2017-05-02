package com.searchmetrics.simpleEmailService.api.rest;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.GetSendStatisticsRequest;
import com.amazonaws.services.simpleemail.model.GetSendStatisticsResult;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.searchmetrics.simpleEmailService.ServiceMetrics;
import com.searchmetrics.simpleEmailService.dto.SendEmailRequest;
import com.searchmetrics.simpleEmailService.dto.SendStatistics;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/")
public class SimpleEmailServiceEndpoint {
    private ServiceMetrics serviceMetrics;
    private final AWSCredentials CREDENTIALS;
    private AmazonSimpleEmailServiceClient client;

    public SimpleEmailServiceEndpoint(ServiceMetrics serviceMetrics) {
        this.serviceMetrics = serviceMetrics;

        try {
            CREDENTIALS = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (~/.aws/credentials), and is in valid format.", e
            );
        }

        client = new AmazonSimpleEmailServiceClient(CREDENTIALS);
        Region REGION = Region.getRegion(Regions.EU_WEST_1);
        client.setRegion(REGION);
    }

    @POST
    @Path("sendEmail")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendEmail(SendEmailRequest emailRequest) {
        try {
            // convert the parsed dto to a sendable email for AWS
            SendRawEmailRequest rawEmailRequest = emailRequest.toAWSRawEmailRequest();

            // send the email
            client.sendRawEmail(rawEmailRequest);

//        } catch (IllegalArgumentException e) {
//            return Response
//                    .status(400)
//                    .entity(new SendEmailResponse("E-Mail was not send: " + e.getMessage()))
//                    .build();
//        } catch (InternalServerErrorException e) {
//            return Response
//                    .serverError()
//                    .entity(new SendEmailResponse("E-Mail was not send: " + e.getMessage()))
//                    .build();
        } catch (Exception e) {
            serviceMetrics.markSendEmailException();

            return Response
                    .serverError()
                    .entity(new SendEmailResponse("E-Mail was not send: " + e.getMessage()))
                    .build();
        }

        // return 200 code and
        return Response.ok().entity(new SendEmailResponse("E-Mail was sent.")).build();
    }

    static class SendEmailResponse {
        private final String STATUS_MESSAGE;

        public SendEmailResponse(String statusMessage) {
            STATUS_MESSAGE = statusMessage;
        }

        @JsonProperty
        public String getStatusMessage() {
            return STATUS_MESSAGE;
        }
    }

    @GET
    @Path("sendStatistics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSendStatistics() {
        //
        // Get send statistics from AWS
        //

        GetSendStatisticsRequest statsRequest = new GetSendStatisticsRequest();
        GetSendStatisticsResult statsResponse = client.getSendStatistics(statsRequest);

        // Create a new send statistics dto from the response
        SendStatistics sendStatistics = new SendStatistics(statsResponse);

        return Response.ok().entity(sendStatistics).build();
    }
}