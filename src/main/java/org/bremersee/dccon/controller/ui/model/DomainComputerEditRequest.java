package org.bremersee.dccon.controller.ui.model;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.model.DomainComputer;
import org.ldaptive.dn.Dn;

@Data
@NoArgsConstructor
public class DomainComputerEditRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private DomainComputer computer;

  private String newOu;

  public DomainComputerEditRequest(DomainComputer computer, Dn newOu) {
    this.computer = computer;
    this.newOu = newOu.format();
  }

  public Dn getNewOuDn() {
    if (isEmpty(newOu)) {
      return null;
    }
    return new Dn(newOu);
  }

}
