package com.archanchor.services;

import com.archanchor.domains.clazz.Clazz;
import com.archanchor.domains.dependency.Dependency;
import com.archanchor.domains.dependency.Type;
import com.archanchor.domains.module.Module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The classes of one module, real or simulated, and which of their references stay inside
 * it (#43). The parser drops references within a package, so a module read from disk
 * never depends on its own classes. A class moved or merged into a module keeps the
 * references it was parsed with: within the module, those to its other classes, or to
 * types nested in them, are internal, as they would be once the code was actually moved.
 * Classes of unknown package are never matched, so they keep every reference.
 *
 * <pre>
 * ModuleScope scope = ModuleScope.of(targetModule).with(classToMove);
 * List&lt;Dependency&gt; leaving = scope.dependenciesOf(classToMove);
 * </pre>
 */
public final class ModuleScope {

	private final Set<String> classNames;

	// A scope lives for one computation over one module, which compares every pair of its
	// classes, so each class's packages are worked out once. By identity, as the analyses
	// compare classes.
	private final Map<Clazz, Set<String>> packagesByClass = new IdentityHashMap<>();

	private ModuleScope(Set<String> classNames) {
		this.classNames = classNames;
	}

	public static ModuleScope of(Module module) {
		return of(module.getClazzes() != null ? module.getClazzes() : List.of());
	}

	public static ModuleScope of(Collection<Clazz> clazzes) {
		return new ModuleScope(clazzes.stream()
			.map(Clazz::qualifiedName)
			.filter(Objects::nonNull)
			.collect(Collectors.toUnmodifiableSet()));
	}

	/** This scope with one more class, e.g. one a simulation moves in. */
	public ModuleScope with(Clazz joining) {
		Set<String> names = new HashSet<>(classNames);
		if (joining.qualifiedName() != null) {
			names.add(joining.qualifiedName());
		}
		return new ModuleScope(names);
	}

	/**
	 * The class's dependencies that leave the module. A dependency some of whose types
	 * are internal keeps the others; one whose types are all internal is dropped. The
	 * class's own list is never changed: the same instance belongs to the real module and
	 * to every simulated one.
	 */
	public List<Dependency> dependenciesOf(Clazz clazz) {
		if (clazz.getDependencies() == null) {
			return List.of();
		}
		if (classNames.isEmpty()) {
			return clazz.getDependencies();
		}
		List<Dependency> leaving = new ArrayList<>();
		for (Dependency dependency : clazz.getDependencies()) {
			external(dependency).ifPresent(leaving::add);
		}
		return leaving;
	}

	/** The packages of {@link #dependenciesOf(Clazz)}. */
	public Set<String> packagesOf(Clazz clazz) {
		return packagesByClass.computeIfAbsent(clazz,
				c -> dependenciesOf(c).stream()
					.map(Dependency::getPackageName)
					.filter(Objects::nonNull)
					.collect(Collectors.toUnmodifiableSet()));
	}

	private Optional<Dependency> external(Dependency dependency) {
		List<Type> types = dependency.getTypes();
		if (types == null || types.isEmpty()) {
			return Optional.of(dependency);
		}
		List<Type> external = types.stream().filter(type -> !isInternal(type.getFullyQualifiedName())).toList();
		if (external.size() == types.size()) {
			return Optional.of(dependency);
		}
		if (external.isEmpty()) {
			return Optional.empty();
		}
		Dependency kept = new Dependency(dependency.getPackageName());
		external.forEach(kept::addType);
		return Optional.of(kept);
	}

	// A class of the module, or a type nested in one: shop.orders.Order.Status is inside
	// shop.orders.Order.
	private boolean isInternal(String typeName) {
		for (String name = typeName; name != null; name = enclosing(name)) {
			if (classNames.contains(name)) {
				return true;
			}
		}
		return false;
	}

	private static String enclosing(String typeName) {
		int lastDot = typeName.lastIndexOf('.');
		return lastDot > 0 ? typeName.substring(0, lastDot) : null;
	}

}
