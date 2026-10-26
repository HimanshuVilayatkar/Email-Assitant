package com.email.writer.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiURL;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient webClient) {
        this.webClient = webClient;
    }


     public String genreateEmailReply(EmailRequest emailRequest){
         //build the prompt
         String prompt=buildPrompt(emailRequest);
         //craft a request

         Map<String, Object> requestBody = Map.of(
                 "model", "gemini-3.8-flash",
                 "input", prompt
         );

         //Do request and get response

         String response=webClient.post()
                 .uri(geminiApiURL)
                 .header("Content-Type", "application/json")
                 .header("x-goog-api-key", geminiApiKey)
                 .bodyValue(requestBody)
                 .retrieve()
                 .bodyToMono(String.class)
                 .block();


         //return response
         return extractResponseContent(response);

     }

    private String buildPrompt(EmailRequest emailRequest) {
         StringBuilder prompt=new StringBuilder();
         prompt.append("Genrate a professional email reply for the following email content. Please don't generate a subject line ");
         if (emailRequest.getTone()!=null && !emailRequest.getTone().isEmpty()){
             prompt.append("Use a").append(emailRequest.getTone()).append(" tone.");
         }
         prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());

         return prompt.toString();

    }
    private String extractResponseContent(String response){
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            for (JsonNode step : rootNode.path("steps")) {
                if ("model_output".equals(step.path("type").asText())) {
                    return step.path("content").get(0).path("text").asText();
                }
            }
            return "No model output found";

        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }
}
