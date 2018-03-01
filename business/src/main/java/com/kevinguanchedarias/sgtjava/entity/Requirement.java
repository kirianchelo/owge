package com.kevinguanchedarias.sgtjava.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

/**
 * @author Kevin Guanche Darias
 *
 */
@Entity
@Table(name = "requirements")
public class Requirement implements SimpleIdEntity {

	private static final long serialVersionUID = 418080945672588722L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String code;
	private String description;

	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
