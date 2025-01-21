package org.bremersee.dccon.converter;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.exception.ServiceException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ToSambaToolValueTransformers {

  private static final String FQDN_DNS = "ns";

  private static final String FQDN_MAIL = "email";

  private static final String SERIAL = "serial";

  private static final String REFRESH = "refresh";

  private static final String RETRY = "retry";

  private static final String EXPIRE = "expire";

  private static final String MINIMUM_TTL = "minttl";

  public static final Function<String, String> FQDN_TRANSFORMER = fqdn -> {
    if (isEmpty(fqdn)) {
      throw ServiceException.badRequest("FQDN must be present.",
          ErrorCode.EC_DNS_FQDN_MISSING);
    }
    if (!fqdn.endsWith(".")) {
      return fqdn + '.';
    }
    return fqdn;
  };

  public static final Function<String, String> SOA_TRANSFORMER = soa -> {
    if (isEmpty(soa)) {
      throw ServiceException.badRequest("SOA value must be present.",
          ErrorCode.EC_DNS_SOA_MISSING);
    }
    // input: serial=7, refresh=900, retry=600, expire=86400, minttl=3600, ns=ns1.exampleorg., email=hostmaster.example.org.
    // output: "fqdn_dns fqdn_email serial refresh retry expire minimumttl"
    return '"'
        + FQDN_TRANSFORMER.apply(getSoaValue(FQDN_DNS, soa)) + " "
        + FQDN_TRANSFORMER.apply(getSoaValue(FQDN_MAIL, soa)) + " "
        + getSoaValue(SERIAL, soa) + " "
        + getSoaValue(REFRESH, soa) + " "
        + getSoaValue(RETRY, soa) + " "
        + getSoaValue(EXPIRE, soa) + " "
        + getSoaValue(MINIMUM_TTL, soa)
        + '"';
  };

  private static String getSoaValue(String name, String input) {
    int start = input.indexOf(name + "=");
    if (start == -1) {
      throw ServiceException.badRequest(String.format("SOA entry '%s' was not found.", name),
          ErrorCode.EC_DNS_SOA_MISSING + '-' + name);
    }
    int end = input.indexOf(',', start);
    if (end == -1) {
      end = input.length();
    }
    return input.substring(start + name.length() + 1, end);
  }

  public static final Function<String, String> MX_TRANSFORMER = mx -> {
    if (isEmpty(mx)) {
      throw ServiceException.badRequest("MX value must be present.",
          ErrorCode.EC_DNS_MX_MISSING);
    }
    // input: mail.example.org. (preference)
    String fqdn;
    String pref;
    int index = mx.indexOf(' ');
    if (index > 0) {
      fqdn = mx.substring(0, index).trim();
      pref = mx.substring(index + 1).trim();
      if (pref.startsWith("(") && pref.endsWith(")")) {
        pref = pref.substring(1, pref.length() - 1).trim();
      }
    } else {
      fqdn = mx.trim();
      pref = "0";
    }
    // output: "fqdn_string preference"
    return '"'
        + FQDN_TRANSFORMER.apply(fqdn)
        + ' '
        + pref
        + '"';
  };

  public static final Function<String, String> SRV_TRANSFORMER = srv -> {
    if (isEmpty(srv)) {
      throw ServiceException.badRequest("SRV value must be present.",
          ErrorCode.EC_DNS_SRV_MISSING);
    }
    // input: foo.example.org. (port, priority, weight)
    String fqdn;
    String port;
    String priority;
    String weight;
    int index = srv.indexOf(' ');
    if (index > 0) {
      fqdn = srv.substring(0, index).trim();
      String tmp = srv.substring(index + 1).trim();
      if (tmp.startsWith("(") && tmp.endsWith(")")) {
        tmp = tmp.substring(1, tmp.length() - 1).trim();
      }
      String[] values = tmp.split(",");
      if (values.length > 0) {
        port = values[0].trim();
      } else {
        port = "0";
      }
      if (values.length > 1) {
        priority = values[1].trim();
      } else {
        priority = "0";
      }
      if (values.length > 2) {
        weight = values[2].trim();
      } else {
        weight = "0";
      }

    } else {
      fqdn = srv.trim();
      port = "0";
      priority = "0";
      weight = "0";
    }
    // output: "fqdn_string port priority weight"
    return '"'
        + FQDN_TRANSFORMER.apply(fqdn) + ' '
        + port + ' '
        + priority + ' '
        + weight
        + '"';
  };

  public static final Function<String, String> TXT_TRANSFORMER = txt -> {
    if (isEmpty(txt)) {
      throw ServiceException.badRequest("TXT value must be present.",
          ErrorCode.EC_DNS_TXT_MISSING);
    }
    // input: "string1","string2","string3"
    String parameter = Arrays.stream(txt.split(Pattern.quote("\",\"")))
        .map(String::trim)
        .map(value -> {
          if (value.startsWith("\"")) {
            return value.substring(1).trim();
          }
          return value;
        })
        .map(value -> {
          if (value.endsWith("\"")) {
            return value.substring(0, value.length() - 1).trim();
          }
          return value;
        })
        .map(String::trim)
        .map(value -> "'" + value + "'")
        .collect(Collectors.joining(" "));
    // output: "'string1' 'string2' ..."
    return '"' + parameter + '"';
  };

}
