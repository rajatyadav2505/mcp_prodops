package com.prodops.controltower.mcp.domain.port;

import com.prodops.controltower.mcp.domain.model.BitbucketChange;
import com.prodops.controltower.mcp.domain.model.BitbucketChangeQuery;
import java.util.List;

public interface BitbucketPort {

  List<BitbucketChange> listChanges(BitbucketChangeQuery query);
}
