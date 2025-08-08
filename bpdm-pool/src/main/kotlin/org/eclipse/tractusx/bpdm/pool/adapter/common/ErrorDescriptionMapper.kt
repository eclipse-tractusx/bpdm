/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.adapter.common

import org.eclipse.tractusx.bpdm.pool.dto.validation.error.*
import org.springframework.stereotype.Service

@Service
class ErrorDescriptionMapper {

    fun getDescription(scope: String, error: AddressCreateContentError): String{
        return when(error){
            is AddressCreateContentError.AddressContent -> getDescription(scope, error.addressContent)
            is AddressCreateContentError.Parent -> getDescription(scope, error.parent)
        }
    }

    fun getDescription(scope: String, error: AddressParentError): String{
        val newScope = "$scope.Parent"
        return when(error){
            is AddressParentError.Invalid -> "$newScope: BPN is invalid."
            is AddressParentError.IsMissing -> getDescription(newScope, error.isMissing)
            is AddressParentError.LegalEntity -> getDescription(newScope, error.legalEntity)
            is AddressParentError.Site -> getDescription(newScope, error.site)
        }
    }

    fun getDescription(scope: String, error: AddressContentError): String{
        return when(error){
            is AddressContentError.AlternativeAddress -> getDescription(scope, error.alternativeAddress)
            is AddressContentError.BusinessState -> getDescription(scope, error.businessStateError)
            is AddressContentError.Confidence -> getDescription(scope, error.confidence)
            is AddressContentError.Identifier -> getDescription(scope, error.identifierError)
            is AddressContentError.Name -> getDescription(scope, error.name)
            is AddressContentError.PhysicalAddress ->  getDescription(scope, error.physicalAddressError)
            is AddressContentError.IdentifierList -> getDescription(scope, error.exceedLengthError)
        }
    }

    fun getDescription(scope: String, error: BusinessStateError): String{
        return when(error){
            is BusinessStateError.Type -> getDescription("$scope.${error::class.simpleName}.Index ${error.index}", error.type)
        }
    }

    fun getDescription(scope: String, error: IdentifierError): String{
        val newScope = "$scope.Identifier.Index ${error.index}.${error::class.simpleName}"
        return when(error){
            is IdentifierError.Duplicate -> "$newScope: Identifier is duplicate."
            is IdentifierError.IssuingService -> getDescription(newScope, error.issuingService)
            is IdentifierError.Type -> getDescription(newScope, error.type)
            is IdentifierError.Value -> getDescription(newScope, error.value)
        }
    }

    fun getDescription(scope: String, error: ConfidenceError): String{
        val newScope = "$scope.${error::class.simpleName}"
        return when(error){
            is ConfidenceError.CheckedExternally -> getDescription(newScope, error.checkedExternally)
            is ConfidenceError.LastCheck -> getDescription(newScope, error.lastCheck)
            is ConfidenceError.Level -> getDescription(newScope, error.level)
            is ConfidenceError.NextCheck -> getDescription(newScope, error.nextCheck)
            is ConfidenceError.SharedByOwner -> getDescription(newScope, error.sharedByOwner)
            is ConfidenceError.SharingMemberAmount -> getDescription(newScope, error.sharingMemberAmount)
        }
    }

    fun getDescription(scope: String, error: PhysicalAddressError): String{
        val baseScope = "$scope.PhysicalAddress"
        val newScope = "$baseScope.${error::class.simpleName}"
        return when(error){
            is PhysicalAddressError.AdminAreaLevel2 -> getDescription(newScope, error.adminAreaLevel2)
            is PhysicalAddressError.AdminAreaLevel3 ->  getDescription(newScope, error.adminAreaLevel3)
            is PhysicalAddressError.Building ->  getDescription(newScope, error.building)
            is PhysicalAddressError.CompanyPostCode ->  getDescription(newScope, error.companyPostCode)
            is PhysicalAddressError.District ->  getDescription(newScope, error.district)
            is PhysicalAddressError.Door ->  getDescription(newScope, error.door)
            is PhysicalAddressError.Floor ->  getDescription(newScope, error.floor)
            is PhysicalAddressError.IndustrialZone ->  getDescription(newScope, error.industrialZone)
            is PhysicalAddressError.PostalAddress ->  getDescription(baseScope, error.postalAddress)
            is PhysicalAddressError.Street ->  getDescription(newScope, error.street)
            is PhysicalAddressError.TaxJurisdiction ->  getDescription(newScope, error.taxJurisdiction)
        }
    }

    fun getDescription(scope: String, error: AlternativeAddressError): String{
        val baseScope = "$scope.AlternativeAddress"
        val newScope = "$baseScope.${error::class.simpleName}"
        return when(error){
            is AlternativeAddressError.DeliveryServiceNumber -> getDescription(newScope, error.deliveryServiceNumber)
            is AlternativeAddressError.DeliveryServiceQualifier -> getDescription(newScope, error.deliveryServiceQualifier)
            is AlternativeAddressError.DeliveryServiceType -> getDescription(newScope, error.deliveryServiceType)
            is AlternativeAddressError.PostalAddress -> getDescription(baseScope, error.postalAddress)
        }
    }

    fun getDescription(scope: String, error: StreetError): String{
        val newScope = "$scope.Street.${error::class.simpleName}"
        return when(error){
            is StreetError.AdditionalNamePrefix -> getDescription(newScope, error.additionalNamePrefix)
            is StreetError.AdditionalNameSuffix -> getDescription(newScope, error.additionalNameSuffix)
            is StreetError.Direction -> getDescription(newScope, error.direction)
            is StreetError.HouseNumber -> getDescription(newScope, error.houseNumber)
            is StreetError.HouseNumberSupplement -> getDescription(newScope, error.houseNumberSupplement)
            is StreetError.Milestone -> getDescription(newScope, error.milestone)
            is StreetError.Name -> getDescription(newScope, error.name)
            is StreetError.NamePrefix -> getDescription(newScope, error.namePrefix)
            is StreetError.NameSuffix -> getDescription(newScope, error.nameSuffix)
        }
    }

    fun getDescription(scope: String, error: PostalAddressError): String{
        val newScope = "$scope.${error::class.simpleName}"
        return when(error){
            is PostalAddressError.AdminArea -> getDescription(newScope, error.adminArea)
            is PostalAddressError.City -> getDescription(newScope, error.city)
            is PostalAddressError.Country -> getDescription(newScope, error.country)
            is PostalAddressError.GeoData -> getDescription(newScope, error.geoData)
            is PostalAddressError.PostCode -> getDescription(newScope, error.postCode)
        }
    }

    fun getDescription(scope: String, error: GeoDataError): String{
        return when(error){
            is GeoDataError.Latitude -> getDescription(scope, error.latitude)
            is GeoDataError.Longitude -> getDescription(scope, error.longitude)
        }
    }

    fun getDescription(scope: String, error: RequiredStringError): String{
        return when(error){
            is RequiredStringError.ExceedLength -> getDescription(scope, error.error)
            is RequiredStringError.IsMissing -> getDescription(scope, error.error)
        }
    }

    fun getDescription(scope: String, error: RequiredMatchError): String{
        return when(error){
            is RequiredMatchError.NoMatch -> getDescription(scope, error.noMatch)
            is RequiredMatchError.IsMissing -> getDescription(scope, error.isMissing)
        }
    }

    fun getDescription(scope: String, error: ExceedLengthError): String{
        return "$scope: Exceeds length of ${error.maxLength}."
    }

    fun getDescription(scope: String, @Suppress("unused") error: IsMissingError): String{
        return "$scope: Is required but missing."
    }

    fun getDescription(scope: String, @Suppress("unused") error: NoMatchError): String{
        return "$scope: Can not be found."
    }

}