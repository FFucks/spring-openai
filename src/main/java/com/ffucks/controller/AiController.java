package com.ffucks.controller;

import com.ffucks.dto.ReviewRequest;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.stabilityai.api.StabilityAiImageOptions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final ChatClient chat;
    private final ImageModel imageModel;

    public AiController(ChatClient.Builder chatClientBuilder, ImageModel imageModel) {
        this.chat = chatClientBuilder.build();
        this.imageModel = imageModel;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "Explain what it is Spring AI em 3 terms") String message) {
        return chat.prompt().user(message).call().content();
    }

    @GetMapping("/chat-response")
    public ChatResponse chatResponse(@RequestParam(defaultValue = "Explain what it is Spring AI em 3 terms") String message) {
        return chat.prompt().user(message).call().chatResponse();
    }

    @PostMapping("/movie-review")
    public String review(@RequestBody @Valid ReviewRequest req) {
        var tpl = new PromptTemplate("Make a resume of the movie {movie} and tell me about the work of the director.");
        tpl.add("movie", req.movie());
        return chat.prompt().user(tpl.render()).call().content();
    }

    @GetMapping(value = "/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> image(@RequestParam String prompt) {
        var opts = StabilityAiImageOptions.builder()
                .width(1024).height(1024)
                .stylePreset("cinematic")
                .N(1)
                .build();

        ImageResponse resp = imageModel.call(new ImagePrompt(prompt, opts));

        // getResult() => ImageGeneration; getOutput() => Image (unic)
        Image image = resp.getResult().getOutput();

        byte[] png;
        if (image.getB64Json() != null && !image.getB64Json().isBlank()) {
            png = Base64.getDecoder().decode(image.getB64Json());
        } else if (image.getUrl() != null && !image.getUrl().isBlank()) {
            png = downloadImage(image.getUrl());
        } else {
            throw new IllegalStateException("No message returned.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"image.png\"")
                .body(png);
    }

    private byte[] downloadImage(String url) {
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).GET().build();
            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("Error on download image from URL: " + url, e);
        }
    }
}
