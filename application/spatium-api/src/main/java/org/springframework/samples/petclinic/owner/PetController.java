/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.util.Collection;
import java.util.Map;

import javax.validation.Valid;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
@RequestMapping("/owners/{ownerId}")
class PetController {

	private static final String VIEWS_PETS_CREATE_OR_UPDATE_FORM = "pets/createOrUpdatePetForm";

	private final PetRepository pets;

	private final OwnerRepository owners;

	public PetController(PetRepository pets, OwnerRepository owners) {
		this.pets = pets;
		this.owners = owners;
	}

	@ModelAttribute("types")
	public Collection<PetType> populatePetTypes() {
		return this.pets.findPetTypes();
	}

	@ModelAttribute("owner")
	public Mono<Owner> findOwner(@PathVariable("ownerId") int ownerId) {
		return Mono.fromCallable(() -> this.owners.findById(ownerId)).subscribeOn(Schedulers.elastic());
	}

	@InitBinder("owner")
	public void initOwnerBinder(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@InitBinder("pet")
	public void initPetBinder(WebDataBinder dataBinder) {
		dataBinder.setValidator(new PetValidator());
	}

	@GetMapping("/pets/new")
	public String initCreationForm(Owner owner, Map<String, Object> model) {
		Pet pet = new Pet();
		owner.addPet(pet);
		model.put("pet", pet);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/new")
	public Mono<String> processCreationForm(Owner owner, @Valid Pet pet, BindingResult result,
			Map<String, Object> model) {
		if (StringUtils.hasLength(pet.getName()) && pet.isNew() && owner.getPet(pet.getName(), true) != null) {
			result.rejectValue("name", "duplicate", "already exists");
		}
		owner.addPet(pet);
		if (result.hasErrors()) {
			model.put("pet", pet);
			return Mono.just(VIEWS_PETS_CREATE_OR_UPDATE_FORM);
		}
		else {
			return Mono.fromRunnable(() -> this.pets.save(pet)).subscribeOn(Schedulers.elastic())
					.then(Mono.just("redirect:/owners/{ownerId}"));
		}
	}

	@GetMapping("/pets/{petId}/edit")
	public Mono<String> initUpdateForm(@PathVariable("petId") int petId, Map<String, Object> model) {
		return Mono.fromRunnable(() -> {
			Pet pet = this.pets.findById(petId);
			model.put("pet", pet);
		}).subscribeOn(Schedulers.elastic()).then(Mono.just(VIEWS_PETS_CREATE_OR_UPDATE_FORM));
	}

	@PostMapping("/pets/{petId}/edit")
	public Mono<String> processUpdateForm(@Valid Pet pet, BindingResult result, Owner owner,
			Map<String, Object> model) {
		if (result.hasErrors()) {
			pet.setOwner(owner);
			model.put("pet", pet);
			return Mono.just(VIEWS_PETS_CREATE_OR_UPDATE_FORM);
		}
		else {
			owner.addPet(pet);
			return Mono.fromRunnable(() -> this.pets.save(pet)).subscribeOn(Schedulers.elastic())
					.then(Mono.just("redirect:/owners/{ownerId}"));
		}
	}

}
