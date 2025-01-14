package org.bremersee.dccon.repository;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.model.DomainComputer;
import org.bremersee.dccon.model.TreeSearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

@Validated
public interface DomainComputerRepository {

  Stream<DomainComputer> findAll(
      @Nullable String query,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);

  Optional<DomainComputer> findOne(
      @NotEmpty String name,
      @Nullable Dn ou,
      @Nullable TreeSearchScope searchScope);

  DomainComputer update(@NotNull DomainComputer domainComputer, @Nullable Dn newOu);

  boolean delete(@NotEmpty String name);

}
