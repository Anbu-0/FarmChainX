package com.ecobazaar.ecobazaar.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;


@Entity
@Table(name = "users")
public class User {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column(nullable = false)
	private String name;
	
	@Column(unique = true, nullable = false)
	private String email;
	
	@Column(nullable = false) 
	private String password;
	
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name ="user_roles",
			joinColumns = @JoinColumn(name="user_id"),
			inverseJoinColumns = @JoinColumn(name="role_id")
			)
	private Set<Role> roles = new HashSet<>();
	
	@OneToMany(mappedBy = "farmer", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	
	private List<Product> products = new ArrayList<>();
	
	public User() {}

    public User(String name, String email, String password, Set<Role> roles) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

	

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	public List<Product> getProducts() {
		return products;
	}

	public void setProducts(List<Product> products) {
		this.products = products;
	}
	
	/**
     * Checks if the user has a given role name.
     * Example: user.hasRole("ROLE_FARMER")
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
    }

    /**
     * Returns a list of all role names (e.g., [ROLE_FARMER, ROLE_ADMIN])
     */
    public List<String> getRoleNames() {
        return roles.stream()
                .map(Role::getName)
                .toList();
    }

    @Override
    public String toString() {
        return "User{id=" + id +
                ", email='" + email + '\'' +
                ", roles=" + getRoleNames() +
                '}';
    }
	
	
}