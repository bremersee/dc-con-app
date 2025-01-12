package org.bremersee.dccon.repository;

import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.model.DomainComputer;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("mock")
@Component("domainComputerRepositoryMock")
public class DomainComputerRepositoryMock implements DomainComputerRepository {

  @Override
  public Stream<DomainComputer> findAll(String query, Dn ou, SearchScope searchScope) {
    return Stream.empty();
  }

  @Override
  public Optional<DomainComputer> findOne(String name, Dn ou, SearchScope searchScope) {
    return Optional.empty();
  }

  @Override
  public DomainComputer update(DomainComputer domainComputer, Dn newOu) {
    return null;
  }

  @Override
  public boolean delete(String name) {
    return false;
  }
}
