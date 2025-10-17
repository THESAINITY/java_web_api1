package com.setec.dao;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PutProductDAO {
	private Integer id;
	private String name;
	private double price;
	private int qty;
	private MultipartFile file;
}
