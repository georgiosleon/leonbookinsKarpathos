package com.evgeniradev.javassified.service;

import java.util.List;

import com.evgeniradev.javassified.entity.Region;

public interface RegionService {

	List<Region> findAll();

	Region findByName(String name);

	Region findBySlug(String slug);
	
	Region findById(int id);

}
