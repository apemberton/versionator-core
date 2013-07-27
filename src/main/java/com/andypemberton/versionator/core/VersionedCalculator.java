package com.andypemberton.versionator.core;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

import com.andypemberton.versionator.annotations.Versioned;

/**
 * Calculates excluded fields for a given class at a requested version level
 */
public class VersionedCalculator {

	public Set<String> getExcludedFields(Class requestedClass, String requestedVersion) {
		return getExcludedFields(null, requestedClass, requestedVersion);
	}

	// TODO consider reverting return type back to PropertyDescriptors
	public Set<String> getExcludedFields(String parentProperty, Class requestedClass, String requestedVersion) {
		Set<String> excludedFields = new HashSet<String>();
		try {
			BeanInfo info = Introspector.getBeanInfo(requestedClass);
			for (PropertyDescriptor property : info.getPropertyDescriptors()) {
				Versioned versioned = getVersionedDataForProperty(requestedClass, property);
				if (versioned != null) {
					Version sinceVersion = new Version(versioned.since());
					Version untilVersion = new Version(versioned.until());
					Version version = new Version(requestedVersion);

					if (!version.isValid(sinceVersion, untilVersion)) {
						String p = String.format("%s%s%s", parentProperty, ((parentProperty == null) ? "" : "."), property.getName());
						excludedFields.add(p);
						System.out.println(String.format("excluding @Versioned [%s <= %s >= %s]: %s", sinceVersion.getRawVersion(),
								requestedVersion, untilVersion.getRawVersion(), p));
					}
				}
				// add child fields - TODO optimize performance
				if (shouldCheckChildFields(property, excludedFields)) {
					excludedFields.addAll(this.getExcludedFields(property.getName(), property.getReadMethod().getReturnType(),
							requestedVersion));
				}
			}
		} catch (IntrospectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return excludedFields;
	}

	/**
	 * Determine whether the given property should be checked recursively, ie:
	 * its own properties scanned for Versioned exclusion
	 * 
	 * @param property
	 * @param excludedFields
	 * @return
	 */
	private boolean shouldCheckChildFields(PropertyDescriptor property, Set<String> excludedFields) {
		return !excludedFields.contains(property) && !property.getReadMethod().getReturnType().isPrimitive()
				&& !property.getReadMethod().getReturnType().getName().matches("java\\..*");
	}

	/**
	 * Find the Versioned annotation for a given JavaBean property
	 * 
	 * @param clazz
	 * @param property
	 * @return
	 */
	private Versioned getVersionedDataForProperty(Class clazz, PropertyDescriptor property) {
		Versioned versioned = null;
		try {
			versioned = clazz.getDeclaredField(property.getName()).getAnnotation(Versioned.class);
		} catch (NoSuchFieldException e1) {
			try {
				versioned = clazz.getField(property.getName()).getAnnotation(Versioned.class);
			} catch (NoSuchFieldException e2) {
				// TODO log error - got property but can't get field def
			}
		}

		if (versioned == null) {
			versioned = property.getReadMethod().getAnnotation(Versioned.class);
		}
		return versioned;
	}

}