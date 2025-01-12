package org.bremersee.dccon.model;

import java.util.List;
import org.bremersee.comparator.model.SortOrders;
import org.bremersee.pagebuilder.model.JsonPageDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

public class DomainComputerPage extends JsonPageDto<DomainComputer> {

  public DomainComputerPage() {
  }

  public DomainComputerPage(List<? extends DomainComputer> content, int number, int size,
      long totalElements) {
    super(content, number, size, totalElements);
  }

  public DomainComputerPage(List<? extends DomainComputer> content, int number, int size,
      long totalElements, SortOrders sort) {
    super(content, number, size, totalElements, sort);
  }

  public DomainComputerPage(List<? extends DomainComputer> content, int number, int size,
      long totalElements, Sort sort) {
    super(content, number, size, totalElements, sort);
  }

  public DomainComputerPage(Page<? extends DomainComputer> page) {
    super(page);
  }
}
