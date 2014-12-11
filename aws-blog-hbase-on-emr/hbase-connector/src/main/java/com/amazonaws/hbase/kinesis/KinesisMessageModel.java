/*
 * Copyright 2013-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.hbase.kinesis;

import java.io.Serializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * This is the data model for the objects being sent through the Amazon Kinesis streams in the samples
 * 
 */
@SuppressWarnings("serial")
public class KinesisMessageModel implements Serializable {

    public int userid;
    public String username;
    public String firstname;
    public String lastname;
    public String city;
    public String state;
    public String email;
    public String phone;
    public boolean likesports;
    public boolean liketheatre;
    public boolean likeconcerts;
    public boolean likejazz;
    public boolean likeclassical;
    public boolean likeopera;
    public boolean likerock;
    public boolean likevegas;
    public boolean likebroadway;
    public boolean likemusicals;

    /**
     * Default constructor for Jackson JSON mapper - uses bean pattern.
     */
    public KinesisMessageModel() {

    }

    /**
     * 
     * @param userid
     *        Sample int data field
     * @param username
     *        Sample String data field
     * @param firstname
     *        Sample String data field
     * @param lastname
     *        Sample String data field
     * @param city
     *        Sample String data field
     * @param state
     *        Sample String data field (2 characters)
     * @param email
     *        Sample String data field
     * @param phone
     *        Sample String data field
     * @param likesports
     *        Sample boolean data field
     * @param liketheatre
     *        Sample boolean data field
     * @param likeconcerts
     *        Sample boolean data field
     * @param likejazz
     *        Sample boolean data field
     * @param likeclassical
     *        Sample boolean data field
     * @param likeopera
     *        Sample boolean data field
     * @param likerock
     *        Sample boolean data field
     * @param likevegas
     *        Sample boolean data field
     * @param likebroadway
     *        Sample boolean data field
     * @param likemusicals
     *        Sample boolean data field
     */
    public KinesisMessageModel(int userid,
            String username,
            String firstname,
            String lastname,
            String city,
            String state,
            String email,
            String phone,
            boolean likesports,
            boolean liketheatre,
            boolean likeconcerts,
            boolean likejazz,
            boolean likeclassical,
            boolean likeopera,
            boolean likerock,
            boolean likevegas,
            boolean likebroadway,
            boolean likemusicals) {
        this.userid = userid;
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.city = city;
        this.state = state;
        this.email = email;
        this.phone = phone;
        this.likesports = likesports;
        this.liketheatre = liketheatre;
        this.likeconcerts = likeconcerts;
        this.likejazz = likejazz;
        this.likeclassical = likeclassical;
        this.likeopera = likeopera;
        this.likerock = likerock;
        this.likevegas = likevegas;
        this.likebroadway = likebroadway;
        this.likemusicals = likemusicals;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }

    /**
     * Getter for userid
     * 
     * @return userid
     */
    public int getUserid() {
        return userid;
    }

    /**
     * Setter for userid
     * 
     * @param userid
     *        Value for userid
     */
    public void setUserid(int userid) {
        this.userid = userid;
    }

    /**
     * Getter for username
     * 
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Setter for username
     * 
     * @param username
     *        Value for username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Getter for firstname
     * 
     * @return firstname
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * Setter for firstname
     * 
     * @param firstname
     *        Value for firstname
     */
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    /**
     * Getter for lastname
     * 
     * @return lastname
     */
    public String getLastname() {
        return lastname;
    }

    /**
     * Setter for lastname
     * 
     * @param lastname
     *        Value for lastname
     */
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    /**
     * Getter for city
     * 
     * @return city
     */
    public String getCity() {
        return city;
    }

    /**
     * Setter for city
     * 
     * @param city
     *        Value for city
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * Getter for state
     * 
     * @return state
     */
    public String getState() {
        return state;
    }

    /**
     * Setter for state
     * 
     * @param state
     *        Value for state
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Getter for email
     * 
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Setter for email
     * 
     * @param email
     *        Value for email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Getter for phone
     * 
     * @return phone
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Setter for phone
     * 
     * @param phone
     *        Value for phone
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Getter for likesports
     * 
     * @return likesports
     */
    public boolean isLikesports() {
        return likesports;
    }

    /**
     * Setter for likesports
     * 
     * @param likesports
     *        Value for likesports
     */
    public void setLikesports(boolean likesports) {
        this.likesports = likesports;
    }

    /**
     * Getter for liketheatre
     * 
     * @return liketheatre
     */
    public boolean isLiketheatre() {
        return liketheatre;
    }

    /**
     * Setter for liketheatre
     * 
     * @param liketheatre
     *        Value for liketheatre
     */
    public void setLiketheatre(boolean liketheatre) {
        this.liketheatre = liketheatre;
    }

    /**
     * Getter for likeconcerts
     * 
     * @return likeconcerts
     */
    public boolean isLikeconcerts() {
        return likeconcerts;
    }

    /**
     * Setter for likeconcerts
     * 
     * @param likeconcerts
     *        Value for likeconcerts
     */
    public void setLikeconcerts(boolean likeconcerts) {
        this.likeconcerts = likeconcerts;
    }

    /**
     * Getter for likejazz
     * 
     * @return likejazz
     */
    public boolean isLikejazz() {
        return likejazz;
    }

    /**
     * Setter for likejazz
     * 
     * @param likejazz
     *        Value for likejazz
     */
    public void setLikejazz(boolean likejazz) {
        this.likejazz = likejazz;
    }

    /**
     * Getter for likeclassical
     * 
     * @return likeclassical
     */
    public boolean isLikeclassical() {
        return likeclassical;
    }

    /**
     * Setter for likeclassical
     * 
     * @param likeclassical
     *        Value for likeclassical
     */
    public void setLikeclassical(boolean likeclassical) {
        this.likeclassical = likeclassical;
    }

    /**
     * Getter for likeopera
     * 
     * @return likeopera
     */
    public boolean isLikeopera() {
        return likeopera;
    }

    /**
     * Setter for likeopera
     * 
     * @param likeopera
     *        Value for likeopera
     */
    public void setLikeopera(boolean likeopera) {
        this.likeopera = likeopera;
    }

    /**
     * Getter for likerock
     * 
     * @return likerock
     */
    public boolean isLikerock() {
        return likerock;
    }

    /**
     * Setter for likerock
     * 
     * @param likerock
     *        Value for likerock
     */
    public void setLikerock(boolean likerock) {
        this.likerock = likerock;
    }

    /**
     * Getter for likevegas
     * 
     * @return likevegas
     */
    public boolean isLikevegas() {
        return likevegas;
    }

    /**
     * Setter for likevegas
     * 
     * @param likevegas
     *        Value for likevegas
     */
    public void setLikevegas(boolean likevegas) {
        this.likevegas = likevegas;
    }

    /**
     * Getter for likebroadway
     * 
     * @return likebroadway
     */
    public boolean isLikebroadway() {
        return likebroadway;
    }

    /**
     * Setter for likebroadway
     * 
     * @param likebroadway
     *        Value for likebroadway
     */
    public void setLikebroadway(boolean likebroadway) {
        this.likebroadway = likebroadway;
    }

    /**
     * Getter for likemusicals
     * 
     * @return likemusicals
     */
    public boolean isLikemusicals() {
        return likemusicals;
    }

    /**
     * Setter for likemusicals
     * 
     * @param likemusicals
     *        Value for likemusicals
     */
    public void setLikemusicals(boolean likemusicals) {
        this.likemusicals = likemusicals;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((city == null) ? 0 : city.hashCode());
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((firstname == null) ? 0 : firstname.hashCode());
        result = prime * result + ((lastname == null) ? 0 : lastname.hashCode());
        result = prime * result + (likebroadway ? 1231 : 1237);
        result = prime * result + (likeclassical ? 1231 : 1237);
        result = prime * result + (likeconcerts ? 1231 : 1237);
        result = prime * result + (likejazz ? 1231 : 1237);
        result = prime * result + (likemusicals ? 1231 : 1237);
        result = prime * result + (likeopera ? 1231 : 1237);
        result = prime * result + (likerock ? 1231 : 1237);
        result = prime * result + (likesports ? 1231 : 1237);
        result = prime * result + (liketheatre ? 1231 : 1237);
        result = prime * result + (likevegas ? 1231 : 1237);
        result = prime * result + ((phone == null) ? 0 : phone.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + userid;
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof KinesisMessageModel)) {
            return false;
        }
        KinesisMessageModel other = (KinesisMessageModel) obj;
        if (city == null) {
            if (other.city != null) {
                return false;
            }
        } else if (!city.equals(other.city)) {
            return false;
        }
        if (email == null) {
            if (other.email != null) {
                return false;
            }
        } else if (!email.equals(other.email)) {
            return false;
        }
        if (firstname == null) {
            if (other.firstname != null) {
                return false;
            }
        } else if (!firstname.equals(other.firstname)) {
            return false;
        }
        if (lastname == null) {
            if (other.lastname != null) {
                return false;
            }
        } else if (!lastname.equals(other.lastname)) {
            return false;
        }
        if (likebroadway != other.likebroadway) {
            return false;
        }
        if (likeclassical != other.likeclassical) {
            return false;
        }
        if (likeconcerts != other.likeconcerts) {
            return false;
        }
        if (likejazz != other.likejazz) {
            return false;
        }
        if (likemusicals != other.likemusicals) {
            return false;
        }
        if (likeopera != other.likeopera) {
            return false;
        }
        if (likerock != other.likerock) {
            return false;
        }
        if (likesports != other.likesports) {
            return false;
        }
        if (liketheatre != other.liketheatre) {
            return false;
        }
        if (likevegas != other.likevegas) {
            return false;
        }
        if (phone == null) {
            if (other.phone != null) {
                return false;
            }
        } else if (!phone.equals(other.phone)) {
            return false;
        }
        if (state == null) {
            if (other.state != null) {
                return false;
            }
        } else if (!state.equals(other.state)) {
            return false;
        }
        if (userid != other.userid) {
            return false;
        }
        if (username == null) {
            if (other.username != null) {
                return false;
            }
        } else if (!username.equals(other.username)) {
            return false;
        }
        return true;
    }
}
