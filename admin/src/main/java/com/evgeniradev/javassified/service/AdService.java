package com.evgeniradev.javassified.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.evgeniradev.javassified.entity.Ad;
import com.evgeniradev.javassified.entity.Region;
import com.evgeniradev.validation.ValidAd;

public interface AdService {

	Ad findById(int id);

	Ad findBySlug(String slug);

	void save(ValidAd validAd);
	
	void delete(Ad ad);
	
	Page<Ad> findAllByRegion(Region region, Pageable pageable);

}
