package org.jaaslounge.sso.tomcat.spnego;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * Repr�sente une identit� Active Directory associ�e � un ensemble de r�les.
 * Les r�les sont r�cup�r�s � la vol�e lors du premier appel � hasRole.
 * @author damien
 */
public class SpnegoPrincipal {
	/** r�f�rence vers le principal */
	private Principal principal;
	/** liste des roles obtenus */
	private List roles;
	
	/**
	 * Construit une identit�
	 * @param principal
	 */
	public SpnegoPrincipal(Principal principal) {
		this.principal = principal;
	}
	
	/**
	 * Obtient la r�f�rence vers le principal
	 * @return
	 */
	public Principal getPrincipal() {
		return principal;
	}
	
	/**
	 * Permet de s'assurer que la liste des r�les Active Directory est bien charg�e
	 */
	private void ensureRolesLoaded() {
		if (roles == null) {
			roles = ActiveDirectoryReader.getReader().getRolesForName(principal.getName());
			if (roles == null) roles = new ArrayList();
		}
	}
	
	/**
	 * Permet de savoir si le role indiqu� est contenu dans la liste des r�les r�cup�r�s.
	 * @param role role recherch�
	 * @return vrai si l'utilisateur appartient au role, faux sinon
	 */
	public boolean hasRole(String role) {
		ensureRolesLoaded();
		return roles.contains(role);
	}
}