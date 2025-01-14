package org.bremersee.dccon.service;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.bremersee.dccon.model.DomainComputer;
import org.bremersee.dccon.model.TreeSearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

@Validated
public interface DomainComputerService {

  Page<DomainComputer> getComputers(
      @NotNull Pageable pageable,
      @Nullable String query,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);

  Optional<DomainComputer> getComputer(
      @NotEmpty String name,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);

  DomainComputer updateComputer(@NotNull DomainComputer domainComputer, @Nullable Dn newOu);

  boolean deleteComputer(@NotEmpty String name);

}
