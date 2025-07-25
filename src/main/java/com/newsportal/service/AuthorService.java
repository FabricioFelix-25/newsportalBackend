package com.newsportal.service;

import com.newsportal.dto.AuthorRequest;
import com.newsportal.dto.AuthorResponse;
import com.newsportal.exception.BadRequestException;
import com.newsportal.exception.ResourceNotFoundException;
import com.newsportal.model.Author;
import com.newsportal.repository.AuthorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthorService {

    @Autowired
    private AuthorRepository authorRepository;

    public List<AuthorResponse> getAllAuthors() {
        return authorRepository.findAll().stream()
                .map(AuthorResponse::new)
                .collect(Collectors.toList());
    }

    public AuthorResponse getAuthorById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + id));
        return new AuthorResponse(author);
    }

    public AuthorResponse createAuthor(AuthorRequest request) {
        if (authorRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }

        Author author = new Author();
        author.setName(request.getName());
        author.setEmail(request.getEmail());
        author.setBio(request.getBio());
        author.setAvatarUrl(request.getAvatarUrl());

        Author savedAuthor = authorRepository.save(author);
        return new AuthorResponse(savedAuthor);
    }

    public AuthorResponse updateAuthor(Long id, AuthorRequest request) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + id));

        if (authorRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new BadRequestException("Email is already in use");
        }

        author.setName(request.getName());
        author.setEmail(request.getEmail());
        author.setBio(request.getBio());
        author.setAvatarUrl(request.getAvatarUrl());

        Author savedAuthor = authorRepository.save(author);
        return new AuthorResponse(savedAuthor);
    }

    public void deleteAuthor(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Author not found with id: " + id);
        }
        authorRepository.deleteById(id);
    }
}