package org.bremersee.dccon.service;

import static org.bremersee.comparator.spring.mapper.SortMapper.applyDefaults;

import java.util.Optional;
import org.bremersee.dccon.model.DomainComputer;
import org.bremersee.dccon.model.TreeSearchScope;
import org.bremersee.dccon.repository.DomainComputerRepository;
import org.bremersee.pagebuilder.PageBuilder;
import org.ldaptive.dn.Dn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DomainComputerServiceImpl implements DomainComputerService {

  private final DomainComputerRepository domainComputerRepository;

  public DomainComputerServiceImpl(DomainComputerRepository domainComputerRepository) {
    this.domainComputerRepository = domainComputerRepository;
  }

  @Override
  public Page<DomainComputer> getComputers(
      Pageable pageable,
      String query,
      Dn ou,
      TreeSearchScope searchScope) {

    return new PageBuilder<DomainComputer, DomainComputer>()
        .sourceEntries(domainComputerRepository.findAll(query, ou, searchScope))
        .pageable(applyDefaults(pageable, null, true, null))
        .build();
  }

  @Override
  public Optional<DomainComputer> getComputer(String name, Dn ou, TreeSearchScope searchScope) {
    return domainComputerRepository.findOne(name, ou, searchScope);
  }

  @Override
  public DomainComputer updateComputer(DomainComputer domainComputer, Dn newOu) {
    return domainComputerRepository.update(domainComputer, newOu);
  }

  @Override
  public boolean deleteComputer(String name) {
    return domainComputerRepository.delete(name);
  }
}
