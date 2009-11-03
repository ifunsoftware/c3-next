package org.aphreet.c3.web.dao;

import java.io.Serializable;

import org.aphreet.c3.web.entity.Entity;

public interface ISimpleDao {

	public void persist(Entity e);

	public Entity getEntity(Serializable key,
			Class<? extends Entity> clazz);

	public void delete(Entity e);
}