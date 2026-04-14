package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration class to enable JPA Auditing.
 *
 * <p>JPA Auditing automatically populates audit-related fields
 * such as:
 * <ul>
 *     <li>createdAt</li>
 *     <li>updatedAt</li>
 *     <li>createdBy</li>
 *     <li>updatedBy</li>
 * </ul>
 *
 * <p><b>Requirements:</b>
 * <ul>
 *     <li>Entities must use {@code @EntityListeners(AuditingEntityListener.class)}</li>
 *     <li>Fields must be annotated with:
 *          <ul>
 *              <li>{@code @CreatedDate}</li>
 *              <li>{@code @LastModifiedDate}</li>
 *              <li>{@code @CreatedBy}</li>
 *              <li>{@code @LastModifiedBy}</li>
 *          </ul>
 *     </li>
 *     <li>An {@code AuditorAware} bean must be defined</li>
 * </ul>
 *
 * <p><b>auditorAwareRef:</b>
 * Refers to a bean that provides the current user (e.g., from Spring Security).
 *
 * <p><b>Example AuditorAware:</b>
 * <pre>
 * @Bean
 * public AuditorAware<String> auditorProvider() {
 *     return () -> Optional.of("SYSTEM");
 * }
 * </pre>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {
}