/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.identity.internal.service;

import com.datastax.driver.core.exceptions.InvalidQueryException;
import io.mifos.anubis.api.v1.domain.ApplicationSignatureSet;
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.anubis.config.TenantSignatureRepository;
import io.mifos.core.lang.security.RsaKeyPairFactory;
import io.mifos.identity.internal.repository.SignatureEntity;
import io.mifos.identity.internal.repository.Signatures;
import io.mifos.identity.internal.repository.Tenants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
@Service
public class TenantService implements TenantSignatureRepository {
  private final Tenants tenants;
  private final Signatures signatures;

  @Autowired
  TenantService(final Tenants tenants, final Signatures signatures)
  {
    this.tenants = tenants;
    this.signatures = signatures;
  }

  public Optional<Signature> getIdentityManagerSignature(final String keyTimestamp) {
    final Optional<SignatureEntity> signature = signatures.getSignature(keyTimestamp);
    return signature.map(x -> new Signature(x.getPublicKeyMod(), x.getPublicKeyExp()));
  }

  @Override
  public List<String> getAllSignatureSetKeyTimestamps() {
    return signatures.getAllKeyTimestamps();
  }

  @Override
  public Optional<ApplicationSignatureSet> getSignatureSet(final String keyTimestamp) {
    final Optional<SignatureEntity> signatureEntity = signatures.getSignature(keyTimestamp);
    return signatureEntity.map(this::mapSignatureEntityToApplicationSignatureSet);
  }

  @Override
  public void deleteSignatureSet(final String keyTimestamp) {
    signatures.invalidateEntry(keyTimestamp);
  }

  @Override
  public Optional<Signature> getApplicationSignature(final String keyTimestamp) {
    final Optional<SignatureEntity> signatureEntity = signatures.getSignature(keyTimestamp);
    return signatureEntity.map(x -> new Signature(x.getPublicKeyMod(), x.getPublicKeyExp()));
  }

  public boolean tenantAlreadyProvisioned() {
    try {
      return tenants.currentTenantAlreadyProvisioned();
    }
    catch (final InvalidQueryException e)
    {
      return false;
    }
  }

  public ApplicationSignatureSet createSignatureSet() {
    final RsaKeyPairFactory.KeyPairHolder keys = RsaKeyPairFactory.createKeyPair();
    final SignatureEntity signatureEntity = signatures.add(keys);
    return mapSignatureEntityToApplicationSignatureSet(signatureEntity);
  }

  private ApplicationSignatureSet mapSignatureEntityToApplicationSignatureSet(final SignatureEntity signatureEntity) {
    return new ApplicationSignatureSet(
            signatureEntity.getKeyTimestamp(),
            new Signature(signatureEntity.getPublicKeyMod(), signatureEntity.getPublicKeyExp()),
            new Signature(signatureEntity.getPublicKeyMod(), signatureEntity.getPublicKeyExp()));
  }
}
