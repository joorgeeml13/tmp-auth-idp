package dev.jmatias.account.vo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.net.InetAddress;

@Converter(autoApply = true)
public class IpAddressConverter implements AttributeConverter<IpAddress, InetAddress> {

    @Override
    public InetAddress convertToDatabaseColumn(IpAddress attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public IpAddress convertToEntityAttribute(InetAddress dbData) {
        return dbData == null ? null : new IpAddress(dbData);
    }
}
